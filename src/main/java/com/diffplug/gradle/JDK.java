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
package com.diffplug.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Project;

public class JDK {
	private final File rootFile;
	private final Path rootPath;

	public JDK(Project project) {
		this(new File((String) project.property(KEY_JAVA)));
	}

	public JDK(File rootFile) {
		this.rootFile = rootFile;
		this.rootPath = rootFile.toPath();
	}

	/** The key for the Java home. */
	private static final String KEY_JAVA = "org.gradle.java.home";

	/** Returns the directory at the root of the JDK. */
	public File getBinDir() {
		return rootPath.resolve("bin").toFile();
	}

	/** Returns the directory at the root of the JDK. */
	public File getRootDir() {
		return rootFile;
	}

	/** Returns the JDK's libs which you're going to link against. */
	public List<File> getJdkLibs() {
		Path jreLibBase = rootPath.resolve("jre/lib");
		return Arrays.asList("rt.jar", "jsse.jar", "jce.jar").stream()
				.map(lib -> jreLibBase.resolve(lib).toFile())
				.collect(Collectors.toList());
	}
}
