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
package com.diffplug.gradle.osgi;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GradleIntegrationTest;
import com.diffplug.gradle.ZipMisc;

public class BndManifestPluginTest extends GradleIntegrationTest {
	@Test
	public void assertManifestContentDefaultLocation() throws IOException {
		testCase("", generatedManifest());
	}

	@Test
	public void assertManifestCustomLocation() throws IOException {
		testCase("osgiBndManifest { copyTo 'customlocation' }", generatedManifest());
		Assert.assertEquals(generatedManifest(), read("customlocation"));
	}

	@Test
	public void assertNoMerging() throws IOException {
		write("src/main/resources/META-INF/MANIFEST.MF",
				"Manifest-Version: 1.0",
				"Bundle-ManifestVersion: 2",
				"Bundle-Name: Mock",
				"Bundle-SymbolicName: org.eclipse.e4.demo.e4photo.flickr.mock; singleton:=true",
				"Bundle-Version: 1.0.0.qualifier",
				"Bundle-ActivationPolicy: lazy",
				"Require-Bundle: org.eclipse.core.runtime");
		testCase("", generatedManifest());
	}

	@Test
	public void assertMerging() throws IOException {
		write("src/main/resources/META-INF/MANIFEST.MF",
				"Manifest-Version: 1.0",
				"Bundle-ManifestVersion: 2",
				"Bundle-Name: Mock",
				"Bundle-SymbolicName: org.eclipse.e4.demo.e4photo.flickr.mock; singleton:=true",
				"Bundle-Version: 1.0.0.qualifier",
				"Bundle-ActivationPolicy: lazy",
				"Require-Bundle: org.eclipse.core.runtime");
		String expectedMerge = StringPrinter.buildStringFromLines(
				"Manifest-Version: 1.0",
				"Bundle-ActivationPolicy: lazy",
				"Bundle-ManifestVersion: 2",
				"Bundle-SymbolicName: test",
				"Bundle-Version: 0.0.0.ERRORSETVERSION",
				"Export-Package: test;uses:=\"com.diffplug.common.base\";version=\"0.0.0\"",
				"Import-Package: com.diffplug.common.base;version=\"[3.4,4)\"",
				"Require-Bundle: org.eclipse.core.runtime",
				"Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"");
		testCase("osgiBndManifest { mergeWithExisting true }", expectedMerge);
	}

	@Test
	public void assertCustomJarTasks() throws IOException {
		write("src/main/resources/META-INF/MANIFEST.MF",
				"Manifest-Version: 1.0",
				"Bundle-ManifestVersion: 2",
				"Bundle-Name: Mock",
				"Bundle-SymbolicName: org.eclipse.e4.demo.e4photo.flickr.mock; singleton:=true",
				"Bundle-Version: 1.0.0.qualifier",
				"Bundle-ActivationPolicy: lazy",
				"Require-Bundle: org.eclipse.core.runtime");
		String expectedMerge = StringPrinter.buildStringFromLines(
				"Manifest-Version: 1.0",
				"Bundle-ActivationPolicy: lazy",
				"Bundle-ManifestVersion: 2",
				"Bundle-SymbolicName: test",
				"Bundle-Version: 0.0.0.ERRORSETVERSION",
				"Export-Package: test;uses:=\"com.diffplug.common.base\";version=\"0.0.0\"",
				"Import-Package: com.diffplug.common.base;version=\"[3.4,4)\"",
				"Require-Bundle: org.eclipse.core.runtime",
				"Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"");
		String buildScript = StringPrinter.buildStringFromLines(
				"task customJar(type: Jar) {",
				"  with jar",
				"  manifest.attributes(",
				"    '-exportcontents': 'test.*',",
				"    '-removeheaders': 'Bnd-LastModified,Bundle-Name,Created-By,Tool,Private-Package',",
				"    'Bundle-SymbolicName': 'test'",
				"  )",
				"	classifier 'custom'",
				"}",
				"osgiBndManifest { ",
				"mergeWithExisting true",
				"includeTask 'customJar'",
				"}");

		testCase(buildScript, expectedMerge, "customJar");
	}

	private void testCase(String buildscriptAddendum, String expectedManifest) throws IOException {
		testCase(buildscriptAddendum, expectedManifest, "jar");
	}

	private void testCase(String buildscriptAddendum, String expectedManifest, String task) throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.gradle.osgi.bndmanifest'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies { compile 'com.diffplug.durian:durian:3.4.0' }",
				"jar.manifest.attributes(",
				"  '-exportcontents': 'test.*',",
				"  '-removeheaders': 'Bnd-LastModified,Bundle-Name,Created-By,Tool,Private-Package',",
				"  'Bundle-SymbolicName': 'test'",
				")",
				buildscriptAddendum);
		write("src/main/java/test/Api.java",
				"package test;",
				"",
				"import com.diffplug.common.base.StringPrinter;",
				"",
				"public class Api {",
				"	public static StringPrinter forceImport() {",
				"		return new StringPrinter(str -> {});",
				"	}",
				"}");
		gradleRunner().withArguments(task, "--stacktrace", "-i").build();

		// make sure the jar contains the proper manifest
		File libsDir = file("build/libs");
		File jar = FileMisc.list(libsDir).get(0);
		String manifestContent = ZipMisc.read(jar, "META-INF/MANIFEST.MF");
		Assert.assertEquals(expectedManifest, manifestContent);
	}

	private String generatedManifest() {
		return StringPrinter.buildStringFromLines(
				"Manifest-Version: 1.0",
				"Bundle-ManifestVersion: 2",
				"Bundle-SymbolicName: test",
				"Bundle-Version: 0.0.0.ERRORSETVERSION",
				"Export-Package: test;uses:=\"com.diffplug.common.base\";version=\"0.0.0\"",
				"Import-Package: com.diffplug.common.base;version=\"[3.4,4)\"",
				"Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\"");
	}
}
