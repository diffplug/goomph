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
package com.diffplug.gradle.eclipserunner;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.diffplug.gradle.GradleIntegrationTest;

public class EquinoxLaunchPluginTest extends GradleIntegrationTest {
	@Test
	public void simpleTestCase() throws IOException, InterruptedException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.equinoxlaunch'",
				"}",
				"apply plugin: 'java'",
				"repositories { mavenCentral() }",
				"equinoxLaunch {",
				"    // creates an EquinoxLaunchSetupTask named 'headlessAppSetup'",
				"    headlessAppSetup {",
				"        source.addThisProject()",
				"        source.addMaven('com.google.guava:guava:17.0')",
				"        source.addMaven('com.google.guava:guava:18.0')",
				"        // creates an EquinoxLaunchTask named 'headlessApp' which depends on 'headlessAppSetup'",
				"        launchTask 'headlessApp', {",
				"            it.args = ['-consoleLog', '-application', 'com.diffplug.rcpdemo.headlessapp', 'file', 'test']",
				"        }",
				"    }",
				"}");
		// Not as good as asserting a complete success, but at least it asserts
		// that we don't duplicate the error in #92
		Assertions.assertThat(
				gradleRunner().withArguments("headlessApp")
						.buildAndFail().getOutput())
				.contains("org.eclipse.osgi is required");
	}
}
