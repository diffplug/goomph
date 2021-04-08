/*
 * Copyright (C) 2016-2021 DiffPlug
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


import com.diffplug.gradle.GradleIntegrationTest;
import com.diffplug.gradle.JRE;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class AsMavenPluginTest extends GradleIntegrationTest {
	@Test
	public void simpleTestCase() throws IOException, InterruptedException {
		Assume.assumeTrue(JRE.majorVersion() == 8);
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.p2.asmaven'",
				"}",
				"p2AsMaven {",
				"    group 'eclipse-deps', {",
				"        repoEclipse '4.5.2'",
				"        iu 'org.eclipse.jdt.core'",
				"    }",
				"}",
				"apply plugin: 'java'",
				"dependencies {",
				"    compile 'eclipse-deps:org.eclipse.jdt.core:+'",
				"}");
		gradleRunner().withArguments("jar", "--stacktrace").build();
		Assert.assertTrue(file("build/p2asmaven/p2").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/maven").isDirectory());
	}

	@Test
	public void complexTestCase() throws IOException, InterruptedException {
		Assume.assumeTrue(JRE.majorVersion() == 8);
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.p2.asmaven'",
				"}",
				"def SUPPORTED_VERSIONS = ['4.4.0', '4.5.0', '4.6.0']",
				"p2AsMaven {",
				"    for (version in SUPPORTED_VERSIONS) {",
				"        group 'eclipse-deps-' + version, {",
				"            repoEclipse version",
				"            iu 'javax.inject'",
				"            repo2runnable()",
				"        }",
				"    }",
				"}",
				"apply plugin: 'java'",
				"dependencies {",
				"    compile 'eclipse-deps-4.4.0:org.eclipse.jdt.core:+'",
				"}");
		gradleRunner().withArguments("jar").build();
		Assert.assertTrue(file("build/p2asmaven/p2/eclipse-deps-4.4.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/p2/eclipse-deps-4.5.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/p2/eclipse-deps-4.6.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/p2runnable/eclipse-deps-4.4.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/p2runnable/eclipse-deps-4.5.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/p2runnable/eclipse-deps-4.6.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/maven/eclipse-deps-4.4.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/maven/eclipse-deps-4.5.0").isDirectory());
		Assert.assertTrue(file("build/p2asmaven/maven/eclipse-deps-4.6.0").isDirectory());
	}
}
