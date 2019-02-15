/*
 * Copyright 2019 DiffPlug
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.GradleIntegrationTest;

public class GradleClassicPluginTest extends GradleIntegrationTest {
	@Test
	public void assertClasspathChanged() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse", false);
		Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<classpath>\n" +
				"	<classpathentry path=\"bin/default\" kind=\"output\"/>\n" +
				"	<classpathentry output=\"bin/main\" kind=\"src\" path=\"src/main/java\">\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_scope\" value=\"main\"/>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"main,test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry output=\"bin/main\" kind=\"src\" path=\"src/main/resources\">\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_scope\" value=\"main\"/>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"main,test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry output=\"bin/test\" kind=\"src\" path=\"src/test/java\">\n" +
				"		<attributes>\n" +
				"			<attribute name=\"gradle_scope\" value=\"test\"/>\n" +
				"			<attribute name=\"gradle_used_by_scope\" value=\"test\"/>\n" +
				"		</attributes>\n" +
				"	</classpathentry>\n" +
				"	<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8/\"/>\n" +
				"</classpath>\n",
				plainEclipse);
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.classic", true);
		Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<classpath>\n" +
				"	<classpathentry path=\"bin\" kind=\"output\"/>\n" +
				"	<classpathentry kind=\"src\" path=\"src/main/java\"/>\n" +
				"	<classpathentry kind=\"src\" path=\"src/main/resources\"/>\n" +
				"	<classpathentry kind=\"src\" path=\"src/test/java\"/>\n" +
				"	<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8/\"/>\n" +
				"</classpath>",
				underTestEclipse);
	}

	private String testCase(String pluginId, boolean flip) throws IOException {
		write("src/main/java/pkg/Code.java",
				"package pkg;",
				"public class Code {}");
		write("src/main/resources/resource.txt",
				"data data data");
		write("src/test/java/pkg/Test.java",
				"package pkg;",
				"public class Test {}");
		write("build.gradle",
				"plugins {",
				"    id 'java'",
				"    id '" + pluginId + "'",
				"}");
		gradleRunner().withArguments("eclipse").build();
		String cp = read(".classpath");
		if (flip) {
			String[] linesRaw = cp.replace("\r", "").split("\n");
			List<String> lines = new ArrayList<>(Arrays.asList(linesRaw));
			String outputLine = lines.remove(5);
			lines.add(2, outputLine);
			return lines.stream().collect(Collectors.joining("\n"));
		} else {
			return cp;
		}
	}
}
