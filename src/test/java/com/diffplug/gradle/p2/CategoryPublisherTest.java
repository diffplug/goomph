/*
 * Copyright (C) 2020-2021 DiffPlug
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
package com.diffplug.gradle.p2;


import com.diffplug.gradle.CleanedAssert;
import com.diffplug.gradle.GradleIntegrationTest;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class CategoryPublisherTest extends GradleIntegrationTest {

	// Eclipse version used for application execution
	private static String ECLIPSE_PDE_VERSION = "4.8.0";

	private static String FEATURE_ID = "goomph.test.feature";
	private static String FEATURE_VERSION = "1.1.1";
	private static String FEATURE_JAR_NAME = FEATURE_ID + "_" + FEATURE_VERSION + ".jar";

	private static String PLUGIN_NAME = "goomph.test.plugin";
	private static String PLUGIN_VERSION = "0.2.0";
	private static String PLUGIN_JAR_NAME = PLUGIN_NAME + "_" + PLUGIN_VERSION + ".jar";

	// Directory used as target of the applications
	private static String PROJECT_DIR_PATH = "project";
	private static String PLUGINS_DIR_PATH = PROJECT_DIR_PATH + "/plugins";
	private static String FEATURES_DIR_PATH = PROJECT_DIR_PATH + "/features";

	private static String CATEGORY_FILE_PATH = "category/category.xml";
	private static String CATEGORY_NAME = "TestCategory";

	private static String PUBLISH_CATEGORY_TASK_NAME = "publishCategory";
	private static String PUBLISH_FEATURES_AND_BUNDLES_TASK_NAME = "publishFeaturesAndBundles";

	/**
	 * Tests the update site creation using the {@see FeaturesAndBundlesPublisher}
	 * and {@see CategoryPublisher}
	 **/
	@Test
	public void testCreateUpdateSite() throws IOException {
		CleanedAssert.assumeJre8();
		write(
				"build.gradle",
				"plugins {",
				"    id 'com.diffplug.p2.asmaven'",
				"}",
				"import com.diffplug.gradle.pde.EclipseRelease",
				"import com.diffplug.gradle.p2.CategoryPublisher",
				"import com.diffplug.gradle.p2.FeaturesAndBundlesPublisher",
				"tasks.register('testProjectJar', Jar) {",
				"  archiveFileName = 'test.jar'",
				"  destinationDirectory = file('" + PLUGINS_DIR_PATH + "')",
				"  manifest{attributes('Bundle-SymbolicName': '" + PLUGIN_NAME + "', 'Bundle-Version': '" + PLUGIN_VERSION + "')}",
				"}",
				"tasks.register('" + PUBLISH_FEATURES_AND_BUNDLES_TASK_NAME + "') {",
				"  dependsOn('testProjectJar')",
				"  doLast {",
				"    new FeaturesAndBundlesPublisher().with {",
				"      source(file('" + PROJECT_DIR_PATH + "'))",
				"      inplace()",
				"      append()",
				"      publishArtifacts()",
				"      runUsingBootstrapper()",
				"    }",
				"  }",
				"}",
				"tasks.register('" + PUBLISH_CATEGORY_TASK_NAME + "') {",
				"  doLast {",
				"    new CategoryPublisher(EclipseRelease.official('" + ECLIPSE_PDE_VERSION + "')).with {",
				"      metadataRepository(file('" + PROJECT_DIR_PATH + "'))",
				"      categoryDefinition(file('" + CATEGORY_FILE_PATH + "'))",
				"      runUsingPdeInstallation()",
				"    }",
				"  }",
				"}");

		folder.newFolder(PLUGINS_DIR_PATH);
		folder.newFolder(FEATURES_DIR_PATH);

		writeFeatureXml();
		writeCategoryDefinition();

		/* Execute FeaturesAndBundlesPublisher using the file structure:
		 * project
		 *  - features
		 *   - feature.xml
		 *  - plugins
		 *   - test.jar // created by task 'testProjectJar'
		 */
		gradleRunner().forwardOutput().withArguments(PUBLISH_FEATURES_AND_BUNDLES_TASK_NAME).build();

		// Verify result of FeaturesAndBundlesPublisher application execution
		String artifactsXml = read(PROJECT_DIR_PATH + "/artifacts.xml");
		Assert.assertTrue("FeaturesAndBundles application does not found plugin specified in features.xml",
				artifactsXml.contains("id='goomph.test.plugin'"));
		Assert.assertTrue("FeaturesAndBundles application does not create a feature jar",
				new File(folder.getRoot(), FEATURES_DIR_PATH + '/' + FEATURE_JAR_NAME).exists());
		Assert.assertTrue("FeaturesAndBundles application does not create a plugin jar",
				new File(folder.getRoot(), PLUGINS_DIR_PATH + '/' + PLUGIN_JAR_NAME).exists());

		Pattern categoryContextPattern = Pattern.compile("<property\\s+name='org.eclipse.equinox.p2.name'\\s+value='" + CATEGORY_NAME + "'\\s*/>\\s*" +
				"<property\\s+name='org.eclipse.equinox.p2.type.category'\\s+value='true'\\s*/>");

		String contentXML = read(PROJECT_DIR_PATH + "/content.xml");
		Matcher m = categoryContextPattern.matcher(contentXML);
		Assert.assertFalse("content.xml already contains category metadata", m.find());

		// Execute CategoryPublisher
		gradleRunner().forwardOutput().withArguments(PUBLISH_CATEGORY_TASK_NAME).build();

		// Verify result of CategoryPublisher application execution
		contentXML = read(PROJECT_DIR_PATH + "/content.xml");
		m = categoryContextPattern.matcher(contentXML);
		Assert.assertTrue("CategoryPublisher application does not add the category metadata to content.xml", m.find());
	}

	private void writeFeatureXml() throws IOException {
		write(FEATURES_DIR_PATH + "/feature.xml",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
				"<feature id=\"" + FEATURE_ID + "\" label=\"test\" version=\"" + FEATURE_VERSION + "\">",
				"  <description></description>",
				"  <plugin id=\"" + PLUGIN_NAME + "\"",
				"    download-size=\"0\"",
				"    install-size=\"0\"",
				"    version=\"" + PLUGIN_VERSION + "\"",
				"    unpack=\"false\"/>",
				"</feature>");
	}

	private void writeCategoryDefinition() throws IOException {
		write(CATEGORY_FILE_PATH,
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
				"<site>",
				"  <description url=\"empty url\"></description>",
				"  <feature url=\"\"",
				"     id=\"" + FEATURE_ID + "\"",
				"     version=\"" + FEATURE_VERSION + "\">",
				"    <category name=\"test\"/>",
				"  </feature>",
				"  <category-def name=\"test\"",
				"     label=\"" + CATEGORY_NAME + "\"/>",
				"</site>");
	}
}
