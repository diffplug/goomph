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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.api.tasks.JavaExec;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;

import com.diffplug.common.base.Throwing;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.tree.TreeStream;

/**
 * Easy way to execute code from a Gradle plugin in a separate JVM.
 *
 * Create an class which implements JavaExecable.  It should have some
 * fields which are input, some fields which are output, and a run() method.
 *
 * Here's what happens when you call {@link JavaExecable#exec(Project, JavaExecable)},
 *
 * - Write the `JavaExecable` to a temporary file using {@link Serializable}.
 * - Launch a new JVM with the same classpath as the project's buildscript, and pass the location of that temp file.
 * - The new JVM loads the `JavaExecable` from the temp file, calls run(), writes it back to the temp file, then exits.
 * - Back in gradle, we deserialize the tempfile and return the result.
 * 
 * If the `JavaExecable` happens to throw an exception, it will be transparently
 * rethrown within the calling thread.
 *
 * Example usage:
 * 
 * ```java
 * static class Incrementer implements JavaExecable {
 *     private static final long serialVersionUID = -5728572785844814830L;
 * 
 *     int input;
 *     int output;
 * 
 *     Incrementer(int input) {
 *         this.input = input;
 *     }
 * 
 *     public int getOutput() {
 *         return output;
 *     }
 * 
 *     @Override
 *     public void run() throws Throwable {
 *         output = input + 1;
 *     }
 * }
 * 
 * @Test // obvious
 * public void testInternal() {
 *     Incrementer example = new Incrementer(5);
 *     example.run();
 *     Assert.assertEquals(6, example.output);
 * }
 * 
 * @Test // magic!
 * public void testExternal() throws Throwable {
 *     Incrementer example = new Incrementer(5);
 *     Incrementer result = JavaExecable.execWithoutGradle(example);
 *     Assert.assertEquals(6, result.output);
 * }
 * ```
 *
 */
public interface JavaExecable extends Serializable, Throwing.Runnable {
	static final String BUILDSCRIPT_CLASSPATH = "classpath";

	/**
	 * @param project	the project on which we'll call {@link Project#javaexec(org.gradle.api.Action)}.
	 * @param input		the JavaExecable which we'll take as input and call run() on.
	 * @param settings	any extra settings you'd like to set on the JavaExec (e.g. heap)
	 * @return the JavaExecable after it has had run() called.
	 */
	public static <T extends JavaExecable> T exec(Project project, T input, Throwing.Consumer<JavaExecSpec> settings) throws Throwable {
		// copy the classpath from the project's buildscript (and its parents)
		List<FileCollection> classpaths = TreeStream.toParent(ProjectPlugin.treeDef(), project)
				.map(p -> p.getBuildscript().getConfigurations().getByName(BUILDSCRIPT_CLASSPATH))
				.collect(Collectors.toList());
		// add the gradleApi, workaround from https://discuss.gradle.org/t/gradle-doesnt-add-the-same-dependencies-to-classpath-when-applying-plugins/9759/6?u=ned_twigg
		classpaths.add(project.getConfigurations().detachedConfiguration(project.getDependencies().gradleApi()));
		FileCollection classpathsCombined = new UnionFileCollection(classpaths);

		return execInternal(input, settings, execSpec -> JavaExecWinFriendly.javaExec(project, classpathsCombined, execSpec));
	}

	/** @see #exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer) */
	public static <T extends JavaExecable> T exec(Project project, T input) throws Throwable {
		return exec(project, input, unused -> {});
	}

	/** @see #exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer) */
	public static <T extends JavaExecable> T execWithoutGradle(T input, Throwing.Consumer<JavaExecSpec> settings) throws Throwable {
		ClassLoader classloader = JavaExec.class.getClassLoader();
		@SuppressWarnings("resource")
		URLClassLoader urlClassloader = (URLClassLoader) classloader;
		Set<File> files = new LinkedHashSet<>();
		for (URL url : urlClassloader.getURLs()) {
			String name = url.getFile();
			if (name != null) {
				files.add(new File(name));
			}
		}
		FileCollection classpath = new SimpleFileCollection(files);
		return execInternal(input, settings, execSpec -> JavaExecWinFriendly.javaExecWithoutGradle(classpath, execSpec));
	}

	/** @see #exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer) */
	public static <T extends JavaExecable> T execWithoutGradle(T input) throws Throwable {
		return execWithoutGradle(input, unused -> {});
	}

	/** @see #exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer) */
	@SuppressWarnings("unchecked")
	static <T extends JavaExecable> T execInternal(T input, Throwing.Consumer<JavaExecSpec> settings, Throwing.Function<Throwing.Consumer<JavaExecSpec>, ExecResult> javaExecer) throws Throwable {
		File tempFile = File.createTempFile("JavaExecOutside", ".temp");
		try {
			// write the input object to a file
			write(tempFile, input);

			ExecResult execResult = javaExecer.apply(execSpec -> {
				// use the main below as the main
				execSpec.setMain(JavaExecable.class.getName());
				// pass the input object to the main
				execSpec.args(tempFile.getAbsolutePath());
				// let the user do stuff
				settings.accept(execSpec);
			});
			execResult.rethrowFailure();
			// load the resultant object after it has been executed and resaved
			Object result = read(tempFile);
			if (result instanceof JavaExecable) {
				return (T) result;
			} else if (result instanceof Throwable) {
				// rethrow any exceptions, if there were any
				throw (Throwable) result;
			} else {
				throw Unhandled.classException(result);
			}
		} finally {
			tempFile.delete(); // delete the temp
		}
	}

	/** Main which works in conjunction with {@link JavaExecable#exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer)}. */
	public static void main(String[] args) throws IOException {
		File file = new File(args[0]);
		try {
			// read the target object from the file
			JavaExecable javaExecOutside = read(file);
			// run the object's run method
			javaExecOutside.run();
			// save the object back to file
			write(file, javaExecOutside);
		} catch (Throwable t) {
			// if it's an exception, write it out to file
			write(file, t);
		}
	}

	static <T extends Serializable> void write(File file, T object) throws IOException {
		try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			output.writeObject(object);
		}
	}

	@SuppressWarnings("unchecked")
	static <T extends Serializable> T read(File file) throws ClassNotFoundException, IOException {
		try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			return (T) input.readObject();
		}
	}

	/** Encapsulates whether something is run internally or externally. */
	public enum Mode {
		INTERNAL, EXTERNAL
	}
}
