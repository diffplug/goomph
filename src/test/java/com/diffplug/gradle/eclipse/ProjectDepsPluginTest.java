/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.eclipse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.Diff;
import com.diffplug.gradle.GradleIntegrationTest;

public class ProjectDepsPluginTest extends GradleIntegrationTest {
	@Test
	public void assertClasspathChangedPre2_14() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse", "2.13");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.projectdeps", "2.13");
		// assert the expected thing was added to the .project file
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"DELETE",
				"kind=\"src\" path=\"/a",
				"INSERT",
				"exported=\"true\" path=\"/a\" kind=\"src\" combineaccessrules=\"true"), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	@Test
	public void assertClasspathChangedPost2_14() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse", "2.14-rc-4");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.projectdeps", "2.14-rc-4");
		// assert the expected thing was added to the .project file
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"INSERT",
				" exported=\"true\"",
				"INSERT",
				" combineaccessrules=\"true\""), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	private String testCase(String pluginId, String version) throws IOException {
		write("settings.gradle",
				"include 'a'",
				"include 'b'");
		write("build.gradle",
				"project(':a') {",
				"	apply plugin: 'java'",
				"	apply plugin: 'eclipse'",
				"}",
				"project(':b') {",
				"	apply plugin: 'java'",
				"	dependencies {",
				"		compile project(':a')",
				"	}",
				"}");
		write("b/build.gradle", "plugins { id '" + pluginId + "' }");
		gradleRunner().withGradleVersion(version).withArguments("eclipse").build();
		return read("b/.classpath");
	}

	@Test
	public void assertClasspathBinaryArtifact() throws IOException {
		String baseline = testCaseExistingJars(false, false);
		String replaceJar = testCaseExistingJars(true, false);
		String onlyIfHasJar = testCaseExistingJars(false, true);
		String both = testCaseExistingJars(true, true);
		{ // replaceJar should have removed the jar
			List<String> diff = Arrays.asList(Diff.computeDiff(baseline, replaceJar).split("\n"));
			Assert.assertEquals("DELETE", diff.get(0));
			Assert.assertTrue(diff.get(1).startsWith("<classpathentry path="));
			Assert.assertTrue(diff.get(1).endsWith("/durian-core-1.0.0-sources.jar\"/>"));
		}
		{ // onlyIfHasJar should not add 'other-project'
			Assert.assertEquals(StringPrinter.buildStringFromLines(
					"DELETE",
					"\t<classpathentry exported=\"true\" path=\"/other-project\" kind=\"src\" combineaccessrules=\"true\"/>",
					""), Diff.computeDiff(baseline, onlyIfHasJar));
		}
		{ // both should do both
			List<String> diff = Arrays.asList(Diff.computeDiff(baseline, both).split("\n"));
			Assert.assertEquals("DELETE", diff.get(0));
			Assert.assertTrue(diff.get(1).startsWith("<classpathentry path="));
			Assert.assertTrue(diff.get(1).endsWith("/durian-core-1.0.0-sources.jar\"/>"));
			Assert.assertEquals("DELETE", diff.get(3));
			Assert.assertEquals("\t<classpathentry exported=\"true\" path=\"/other-project\" kind=\"src\" combineaccessrules=\"true\"/>", diff.get(4));
		}
	}

	private String testCaseExistingJars(boolean replaceJar, boolean onlyIfHasJar) throws IOException {
		write("build.gradle",
				"plugins { id 'com.diffplug.gradle.eclipse.projectdeps' }",
				"apply plugin: 'java'",
				"repositories { mavenCentral() }",
				"dependencies { compile 'com.diffplug.durian:durian-core:1.0.0' }",
				"",
				"apply plugin: 'eclipse'",
				"eclipse.project.referencedProjects.add('durian-core')",
				"eclipse.project.referencedProjects.add('other-project')",
				"",
				"eclipseProjectDeps {",
				"    replaceJar = " + replaceJar,
				"    onlyIfHasJar = " + onlyIfHasJar,
				"}");
		gradleRunner().withArguments("jar", "eclipse").forwardOutput().build();
		return read(".classpath");
	}

	@Test
	public void testParseLibraryName() {
		Assert.assertEquals("durian-core-1.0.0", ProjectDepsPlugin.parseLibraryName("C:/Users/ntwigg/AppData/Local/Temp/.gradle-test-kit-ntwigg/caches/modules-2/files-2.1/com.diffplug.durian/durian-core/1.0.0/b4462fca6c0f4ec9e3c38d78c4e57f8575fa1920/durian-core-1.0.0.jar"));
	}
}
