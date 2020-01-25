/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.oomph;


import com.diffplug.gradle.GradleIntegrationTest;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

public class OomphIdePluginTest extends GradleIntegrationTest {
	// This test fails because of how gradle's testkit
	// constructs its classpath.  It uses a separate folder for
	// class files and resource files, rather than the jar'ed result,
	// which means that OSGi can't find the MANIFEST.
	//
	// You can work around this manually by changing the
	// plugin-under-test-metadata.properties file, but it's
	// a hassle to automate.
	//
	// Asked for a workaround / change from Gradle here, we'll see how it goes.
	// https://discuss.gradle.org/t/how-to-make-gradle-testkit-depend-on-output-jar-rather-than-just-classes/18940
	@Ignore
	@Test
	public void assertReadmeExampleWorks() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.oomph.ide'",
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
		gradleRunner().forwardOutput().withArguments("ideSetupP2", "ideSetupWorkspace").build();
	}
}
