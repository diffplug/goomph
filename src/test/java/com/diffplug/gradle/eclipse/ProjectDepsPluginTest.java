/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.eclipse;


import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.Diff;
import com.diffplug.gradle.GradleIntegrationTest;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

public class ProjectDepsPluginTest extends GradleIntegrationTest {
	@Test
	public void assertClasspathChangedPre2_14() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse", "2.13");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.eclipse.projectdeps", "2.13");
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
		String underTestEclipse = testCase("com.diffplug.eclipse.projectdeps", "2.14-rc-4");
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
	public void testReplaceWithProject() throws IOException {
		write("build.gradle",
				"plugins { id 'com.diffplug.eclipse.projectdeps' }",
				"apply plugin: 'java'",
				"repositories { mavenCentral() }",
				"dependencies {",
				"    compile 'com.diffplug.durian:durian-core:1.0.0'",
				"    compile 'com.diffplug.durian:durian-collect:1.0.0'",
				"}",
				"",
				"eclipseProjectDeps {",
				"    replaceWithProject('durian-core')",
				"}");
		gradleRunner().withArguments("eclipse").build();
		String resultRaw = read(".classpath");
		String result = resultRaw
				// replace the system-specific paths
				.replaceAll("\"(?:.*)/(.*?)\"", "$1");
		Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<classpath>\n" +
				"	<classpathentry kind=default/>\n" +
				"	<classpathentry kind=/>\n" +
				"	<classpathentry sourcepath=durian-collect-1.0.0.jar>\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"main,test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry sourcepath=animal-sniffer-annotations-1.14.jar>\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"main,test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry sourcepath=j2objc-annotations-0.1.jar>\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"main,test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry exported=durian-core kind=\"src\" combineaccessrules=\"true\"/>\n" +
				"</classpath>\n", result);
	}

	@Test
	public void testParseLibraryName() {
		Assert.assertEquals("durian-core-1.0.0", ProjectDepsPlugin.parseLibraryName("C:/Users/ntwigg/AppData/Local/Temp/.gradle-test-kit-ntwigg/caches/modules-2/files-2.1/com.diffplug.durian/durian-core/1.0.0/b4462fca6c0f4ec9e3c38d78c4e57f8575fa1920/durian-core-1.0.0.jar"));
	}
}
