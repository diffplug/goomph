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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.GradleIntegrationTest;

public class BndManifestPluginTest extends GradleIntegrationTest {
	@Test
	public void assertManifestMF() throws IOException {
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
				")");
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
		gradleRunner().withArguments("jar", "--stacktrace").build();
		String manifest = read("META-INF/MANIFEST.MF");
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"Manifest-Version: 1.0",
				"Bundle-ManifestVersion: 2",
				"Bundle-SymbolicName: test",
				"Bundle-Version: 0.0.0.ERRORSETVERSION",
				"Export-Package: test;uses:=\"com.diffplug.common.base\";version=\"0.0.0\"",
				"Import-Package: com.diffplug.common.base;version=\"[3.4,4)\"",
				"Require-Capability: osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=1.8))\""), manifest);
	}
}
