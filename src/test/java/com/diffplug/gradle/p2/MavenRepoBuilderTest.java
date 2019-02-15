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
package com.diffplug.gradle.p2;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.io.Files;
import com.diffplug.common.io.Resources;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GradleIntegrationTest;

public class MavenRepoBuilderTest extends GradleIntegrationTest {
	@Test
	public void doTest() throws Exception {
		File bin = copyIntoFolder("org.eclipse.ecf.provider.filetransfer.ssl_1.0.0.v20151130-0157.jar");
		File source = copyIntoFolder("org.eclipse.ecf.provider.filetransfer.ssl.source_1.0.0.v20151130-0157.jar");

		File mavenRoot = new File(folder.getRoot(), "maven");
		try (MavenRepoBuilder builder = new MavenRepoBuilder(mavenRoot)) {
			builder.install("p2group", bin);
			builder.install("p2group", source);
		}
		write("build.gradle",
				"apply plugin: 'java'",
				"repositories { maven { url '" + mavenRoot.getAbsolutePath().replace("\\", "/") + "' } }",
				"dependencies {",
				"    compile 'p2group:org.eclipse.ecf.provider.filetransfer.ssl:+'",
				"}",
				"apply plugin: 'eclipse'");
		gradleRunner().forwardOutput().withArguments("eclipse").build();
		String classpath = read(".classpath");
		Assert.assertTrue(classpath.contains("org.eclipse.ecf.provider.filetransfer.ssl-1.0.0.v20151130-0157.jar\""));
		Assert.assertTrue(classpath.contains("org.eclipse.ecf.provider.filetransfer.ssl-1.0.0.v20151130-0157-sources.jar\""));
	}

	private File copyIntoFolder(String jar) throws IOException {
		URL url = MavenRepoBuilderTest.class.getResource(jar);
		File file = folder.newFile();
		try (OutputStream output = Files.asByteSink(file).openBufferedStream()) {
			Resources.copy(url, output);
		}
		return file;
	}

	public static void main(String[] args) throws Exception {
		File p2 = new File("C:\\Users\\ntwigg\\Documents\\DiffPlugDev\\talk-gradle_and_eclipse_rcp\\targetplatform\\build\\p2asmaven\\p2");
		File maven = new File("C:\\Users\\ntwigg\\Documents\\DiffPlugDev\\talk-gradle_and_eclipse_rcp\\targetplatform\\build\\p2asmaven\\maven");
		try (MavenRepoBuilder builder = new MavenRepoBuilder(maven)) {
			for (File plugin : FileMisc.list(new File(p2, "plugins"))) {
				builder.install("group", plugin);
			}
		}
	}
}
