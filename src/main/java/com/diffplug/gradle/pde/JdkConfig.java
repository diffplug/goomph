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
package com.diffplug.gradle.pde;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.gradle.api.Project;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.collect.ImmutableList;

/**
 * Detects the JDK folder from the gradle project,
 * and wraps it in an API.
 */
public class JdkConfig {
	/** The key for the Java home. */
	private static final String KEY_JAVA = "org.gradle.java.home";

	/** Creates a JDK using the project's `org.gradle.java.home` property. */
	public JdkConfig(Project project) {
		String javaHome;
		if (project.hasProperty(KEY_JAVA)) {
			javaHome = (String) project.property(KEY_JAVA);
		} else {
			javaHome = StandardSystemProperty.JAVA_HOME.value();
		}
		Objects.requireNonNull(javaHome, "Could not find JRE dir, set 'org.gradle.java.home' to fix.");
		// it might point to the root of the JDK folder, or to the JRE itself
		// this handles the case that it is the JRE itself
		File jreSubdir = new File(javaHome + "/jre");
		if (!jreSubdir.exists()) {
			javaHome = jreSubdir.getParentFile().getParentFile().getAbsolutePath();
		}
		this.rootFolder = new File(javaHome);
	}

	public JdkConfig(File rootFile) {
		this.rootFolder = rootFile;
	}

	/** Returns the folder at the root of the JDK. */
	public File getRootFolder() {
		return rootFolder;
	}

	public File rootFolder;
	public String name = "JavaSE-1.8";
	public String source = "1.8";
	public String target = "1.8";
	public List<String> jreLibs = ImmutableList.of(
			"jre/lib/rt.jar",
			"jre/lib/jsse.jar",
			"jre/lib/jce.jar");

	/** Returns the JDK's libs which you're going to link against. */
	public List<File> getJdkLibs() {
		return jreLibs.stream()
				.map(lib -> rootFolder.toPath().resolve(lib).toFile())
				.collect(Collectors.toList());
	}
}
