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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.process.ExecResult;
import org.gradle.process.JavaExecSpec;

import com.diffplug.common.base.Throwing;
import com.diffplug.common.base.Unhandled;

/** Private implementation details. */
class JavaExecableImp {
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

	/** @see #exec(Project, JavaExecable, com.diffplug.common.base.Throwing.Consumer) */
	@SuppressWarnings("unchecked")
	static <T extends JavaExecable> T execInternal(T input, Action<JavaExecSpec> settings, Throwing.Function<Action<JavaExecSpec>, ExecResult> javaExecer) throws Throwable {
		File tempFile = File.createTempFile("JavaExecOutside", ".temp");
		try {
			// write the input object to a file
			JavaExecableImp.write(tempFile, input);

			ExecResult execResult = javaExecer.apply(execSpec -> {
				// use the main below as the main
				execSpec.setMain(JavaExecable.class.getName());
				// pass the input object to the main
				execSpec.args(tempFile.getAbsolutePath());
				// let the user do stuff
				settings.execute(execSpec);
			});
			execResult.rethrowFailure();
			// load the resultant object after it has been executed and resaved
			Object result = JavaExecableImp.read(tempFile);
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
}
