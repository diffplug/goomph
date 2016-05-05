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

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.Diff;
import com.diffplug.gradle.GradleIntegrationTest;

public class ProjectDepsPluginTest extends GradleIntegrationTest {
	@Test
	public void examineXmlChange() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.projectdeps");
		// assert the expected thing was added to the .project file
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"DELETE",
				"kind=\"src\" path=\"/a",
				"INSERT",
				"exported=\"true\" path=\"/a\" kind=\"src\" combineaccessrules=\"true"), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	private String testCase(String pluginId) throws IOException {
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
		gradleRunner().withArguments("eclipse").build();
		return read("b/.classpath");
	}
}
