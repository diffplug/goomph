/*
 * Copyright (C) 2015-2021 DiffPlug
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


import com.diffplug.gradle.GradleIntegrationTest;
import java.io.IOException;
import org.junit.Test;

public class MavenCentralPluginTest extends GradleIntegrationTest {
	@Test
	public void test() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.eclipse.mavencentral'",
				"}",
				"eclipseMavenCentral {",
				"    release '4.7.0', {",
				"        compile 'org.eclipse.equinox.common'",
				"    }",
				"}",
				"apply plugin: 'java'");
		gradleRunner().withArguments("jar", "--stacktrace").build();
	}

	@Test
	public void testICU() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.eclipse.mavencentral'",
				"}",
				"eclipseMavenCentral {",
				"    release '4.7.0', {",
				"        compile 'com.ibm.icu'",
				"    }",
				"}",
				"repositories {",
				"    mavenCentral()",
				"}",
				"apply plugin: 'java'");
		write("src/main/java/pkg/Demo.java",
				"package pkg;",
				"import com.ibm.icu.text.UTF16;",
				"public class Demo {",
				"    public static void main(String[] args) {",
				"        UTF16.isSurrogate('a');",
				"    }",
				"}");
		gradleRunner().withArguments("jar", "--stacktrace").build();
	}
}
