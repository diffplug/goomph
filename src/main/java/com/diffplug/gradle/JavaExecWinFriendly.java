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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.CollectionUtils;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.swt.os.OS;

/**
 * Thanks to Thipor Kong for his workaround for Gradle's windows problems.
 * 
 * https://discuss.gradle.org/t/javaexec-fails-for-long-classpaths-on-windows/15266
 */
public class JavaExecWinFriendly {
	private JavaExecWinFriendly() {}

	/** Calls javaExec() in a way which is friendly with windows classpath limitations. */
	public static ExecResult javaExec(Project project, Action<JavaExecSpec> spec) throws IOException {
		if (OS.getNative().isWindows()) {
			Box.Nullable<File> classpathJarBox = Box.Nullable.ofNull();
			ExecResult execResult = project.javaexec(execSpec -> {
				// handle the user
				spec.execute(execSpec);
				// create a jar which embeds the classpath
				File classpathJar = toJarWithClasspath(execSpec.getClasspath());
				classpathJar.deleteOnExit();
				// set the classpath to be just that one jar
				execSpec.setClasspath(project.files(classpathJar));
				// save the jar so it can be deleted later
				classpathJarBox.set(classpathJar);
			});
			// delete the jar after the task has finished
			Errors.suppress().run(() -> FileMisc.forceDelete(classpathJarBox.get()));
			return execResult;
		} else {
			return project.javaexec(spec);
		}
	}

	/** Calls javaExec() in a way which is friendly with windows classpath limitations, and doesn't require gradle. */
	public static ExecResult javaExecWithoutGradle(Action<JavaExecSpec> spec) throws IOException {
		Project project = ProjectBuilder.builder().build();
		return javaExec(project, spec);
	}

	public static final String LONG_CLASSPATH_JAR_PREFIX = "long-classpath";

	/** Creates a jar with a Class-Path entry to workaround the windows classpath limitation. */
	private static File toJarWithClasspath(Iterable<File> files) {
		return Errors.rethrow().get(() -> {
			File jarFile = File.createTempFile(LONG_CLASSPATH_JAR_PREFIX, ".jar");
			try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(jarFile)))) {
				zip.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
				try (PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8)))) {
					pw.println("Manifest-Version: 1.0");
					String classPath = CollectionUtils.join(" ", CollectionUtils.collect(files, File::toURI));
					String classPathEntry = "Class-Path: " + classPath;
					pw.println(CollectionUtils.join("\n ", classPathEntry.split(MATCH_CHUNKS_OF_70_CHARACTERS)));
				}
			}
			return jarFile;
		});
	}

	private static final String MATCH_CHUNKS_OF_70_CHARACTERS = "(?<=\\G.{70})";
}
