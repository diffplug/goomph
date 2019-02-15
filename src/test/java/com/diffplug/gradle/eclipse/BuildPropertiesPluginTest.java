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

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.Diff;
import com.diffplug.gradle.GradleIntegrationTest;

public class BuildPropertiesPluginTest extends GradleIntegrationTest {
	@Test
	public void assertClasspathChanged() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.buildproperties");
		// assert the expected thing was added to the .project file
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"INSERT",
				"	<classpathentry path=\"\" including=\"META-INF/|images/\" kind=\"src\"/>",
				""), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	private String testCase(String pluginId) throws IOException {
		write("build.properties",
				"source.. = src/",
				"output.. = bin/",
				"bin.includes = .,\\",
				"               META-INF/,\\",
				"               images/",
				"src.excludes = test/");
		write("build.gradle",
				"plugins {",
				"    id 'java'",
				"    id '" + pluginId + "'",
				"}");
		gradleRunner().withArguments("eclipse").build();
		return read(".classpath");
	}
}
