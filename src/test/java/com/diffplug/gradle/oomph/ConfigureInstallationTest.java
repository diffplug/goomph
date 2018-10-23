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
package com.diffplug.gradle.oomph;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

public class ConfigureInstallationTest {

	private String PREFS_FILE = "Contents/Eclipse/configuration/.settings/org.eclipse.core.net.prefs";

	@Test
	public void xml() throws Exception {
		File ideDir = Files.newTemporaryFolder();
		Project project = ProjectBuilder.builder().build();
		OomphIdeExtension extension = new OomphIdeExtension(project);
		extension.ideDir = ideDir.getAbsolutePath();

		File expectedFile = new File(ideDir, PREFS_FILE);

		File exampleXml = new File("src/test/resources/installationExample.xml");
		File exampleXmlInProject = new File(project.getProjectDir(), "installationExample.xml");
		FileUtils.copyFile(exampleXml, exampleXmlInProject);

		extension.installationFile(PREFS_FILE, "installationExample.xml");
		extension.installationXml(PREFS_FILE, new Action<XmlProvider>() {
			@Override
			public void execute(XmlProvider xmlProvider) {
				xmlProvider.asElement().setAttribute("name", "value");
			}
		});
		extension.ideSetupInstallation();

		List<String> expectedContent = FileUtils.readLines(expectedFile, Charset.defaultCharset());
		Assert.assertTrue("Did not find prop in " + expectedContent, expectedContent.contains("<settings name=\"value\">"));
	}

	@Test
	public void file() throws Exception {
		File ideDir = Files.newTemporaryFolder();
		Project project = ProjectBuilder.builder().build();
		OomphIdeExtension extension = new OomphIdeExtension(project);
		extension.ideDir = ideDir.getAbsolutePath();
		File propertyFile = Files.newTemporaryFile();
		FileUtils.write(propertyFile, "systemProxiesEnabled=false", Charset.defaultCharset());

		extension.installationFile(PREFS_FILE, propertyFile);
		extension.ideSetupInstallation();

		File expectedFile = new File(ideDir, PREFS_FILE);
		List<String> expectedContent = FileUtils.readLines(expectedFile, Charset.defaultCharset());
		Assert.assertTrue("Did not find prop", expectedContent.contains("systemProxiesEnabled=false"));
	}

	@Test
	public void props() throws Exception {
		File ideDir = Files.newTemporaryFolder();
		Project project = ProjectBuilder.builder().build();
		OomphIdeExtension extension = new OomphIdeExtension(project);
		extension.ideDir = ideDir.getAbsolutePath();
		extension.installationProp(PREFS_FILE, new Action<Map<String, String>>() {
			@Override
			public void execute(Map<String, String> props) {
				props.put("systemProxiesEnabled", "false");
			}
		});
		extension.ideSetupInstallation();

		File expectedFile = new File(ideDir, PREFS_FILE);
		List<String> expectedContent = FileUtils.readLines(expectedFile, Charset.defaultCharset());
		Assert.assertTrue("Did not find prop", expectedContent.contains("systemProxiesEnabled=false"));
	}
}
