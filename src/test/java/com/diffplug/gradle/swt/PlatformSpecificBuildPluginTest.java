/*
 * Copyright (C) 2021 DiffPlug
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
package com.diffplug.gradle.swt;


import com.diffplug.gradle.GradleIntegrationTest;
import java.io.IOException;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

public class PlatformSpecificBuildPluginTest extends GradleIntegrationTest {
	protected GradleRunner gradleRunner() {
		return super.gradleRunner().withGradleVersion("7.2");
	}

	private GradleRunner breakConfigCache() throws IOException {
		write("gradle.properties", "org.gradle.unsafe.configuration-cache=true");
		write("build.gradle",
				"plugins {",
				"  id 'java'",
				"  id 'com.diffplug.eclipse.mavencentral'",
				"}",
				"repositories { mavenCentral() }",
				"eclipseMavenCentral {",
				"  release '4.20.0', {",
				"    implementation 'org.eclipse.swt'",
				"    implementation 'org.eclipse.jface'",
				"    implementation \"org.eclipse.swt.${com.diffplug.common.swt.os.SwtPlatform.getRunning()}\"",
				"    useNativesForRunningPlatform()",
				"  }",
				"}");
		write("src/main/java/pkg/Demo.java",
				"package pkg;",
				"public class Demo {}");
		return gradleRunner().withArguments("jar");
	}

	@Test
	@Ignore // fails in real project, but not unit test, not sure why
	public void configurationCacheBroken() throws IOException {
		breakConfigCache().buildAndFail();
	}

	@Test
	public void configurationCacheWorks() throws IOException {
		write("settings.gradle",
				"plugins {",
				"  id 'com.diffplug.configuration-cache-for-platform-specific-build'",
				"}");
		breakConfigCache().build();
	}
}
