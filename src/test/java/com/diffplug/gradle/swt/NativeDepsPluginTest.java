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
package com.diffplug.gradle.swt;


import com.diffplug.gradle.GradleIntegrationTest;
import com.diffplug.gradle.JRE;
import java.io.IOException;
import org.junit.Assume;
import org.junit.Test;

public class NativeDepsPluginTest extends GradleIntegrationTest {
	@Test
	public void assertPluginWorks() throws IOException {
		Assume.assumeTrue(JRE.majorVersion() == 8);
		write("build.gradle",
				"plugins {",
				"	id 'java'",
				"	id 'com.diffplug.swt.nativedeps'",
				"}",
				"ext.SWT_VERSION='4.8.0'",
				"repositories { mavenCentral() }",
				"dependencies { testCompile 'junit:junit:4.12' }");
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
