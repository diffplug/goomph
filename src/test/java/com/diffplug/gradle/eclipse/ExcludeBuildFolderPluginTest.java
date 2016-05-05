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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.StringPrinter;

public class ExcludeBuildFolderPluginTest {
	@Rule
	public TemporaryFolder testProjectDir = new TemporaryFolder();

	@Test
	public void examineXmlChange() throws IOException {
		File buildFile = testProjectDir.newFile("build.gradle");
		// write the normal eclipse file
		Files.write(buildFile.toPath(), Arrays.asList("apply plugin: 'eclipse'"));
		GradleRunner.create().withProjectDir(testProjectDir.getRoot()).withArguments("eclipse").withPluginClasspath().build();
		String plainEclipse = getEclipse();
		// write the excluded build folder file
		Files.write(buildFile.toPath(), Arrays.asList("plugins { id 'com.diffplug.gradle.eclipse.excludebuildfolder' }"));
		GradleRunner.create().withProjectDir(testProjectDir.getRoot()).withArguments("eclipse", "--stacktrace").withPluginClasspath().build();
		String underTestEclipse = getEclipse();
		// assert the expected thing was added
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"INSERT",
				"	<filteredResources>",
				"		<filter>",
				"			<id>somenumber</id>",
				"			<name></name>",
				"			<type>10</type>",
				"			<matcher>",
				"				<id>org.eclipse.ui.ide.multiFilter</id>",
				"				<arguments>1.0-name-matches-false-false-build</arguments>",
				"			</matcher>",
				"		</filter>",
				"	</filteredResources>",
				""), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	private String getEclipse() throws IOException {
		File eclipseFile = new File(testProjectDir.getRoot(), ".project");
		return new String(Files.readAllBytes(eclipseFile.toPath()), StandardCharsets.UTF_8)
				.replaceAll("<id>([0-9|-]+)</id>", "<id>somenumber</id>");
	}
}
