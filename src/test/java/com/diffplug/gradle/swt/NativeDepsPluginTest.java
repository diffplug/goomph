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
package com.diffplug.gradle.swt;

import java.io.IOException;

import org.junit.Test;

import com.diffplug.gradle.GradleIntegrationTest;

public class NativeDepsPluginTest extends GradleIntegrationTest {
	@Test
	public void assertPluginWorks() throws IOException {
		testCase("");
	}

	private void testCase(String buildscriptAppend) throws IOException {
		write("build.gradle",
				"plugins {",
				"	id 'java'",
				"	id 'com.diffplug.gradle.swt.nativedeps'",
				"}",
				"repositories { mavenCentral() }",
				"dependencies { testCompile 'junit:junit:4.12' }",
				buildscriptAppend);
		write("src/test/java/undertest/NeedsSwt.java",
				"package undertest;",
				"",
				"import org.junit.*;",
				"",
				"public class NeedsSwt {",
				"	@Test",
				"	public void hasDisplay() {",
				"		Assert.assertEquals(\"org.eclipse.swt.widgets.Display\", org.eclipse.swt.widgets.Display.class.getName());",
				"	}",
				"}");
		gradleRunner().withArguments("test", "--stacktrace").forwardOutput().build();
	}
}
