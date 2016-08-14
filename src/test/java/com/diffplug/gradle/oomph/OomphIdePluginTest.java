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
package com.diffplug.gradle.oomph;

import java.io.IOException;

import org.junit.Test;

import com.diffplug.gradle.GradleIntegrationTest;

public class OomphIdePluginTest extends GradleIntegrationTest {
	@Test
	public void assertManifestContentDefaultLocation() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.oomph.ide'",
				"}",
				"",
				"oomphIde {",
				"	repoEclipseLatest()",
				"	jdt {}",
				"	eclipseIni {",
				"		vmargs('-Xmx2g')  // IDE can have up to 2 gigs of RAM",
				"	}",
				"	style {",
				"		classicTheme()  // oldschool cool",
				"		niceText()      // with nice fonts and visible whitespace",
				"	}",
				"}");
		gradleRunner().forwardOutput().withArguments("ide", "--stacktrace").build();
	}
}
