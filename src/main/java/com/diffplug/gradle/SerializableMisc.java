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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/** Utilities for manipulating {@link Serializable} objects. */
public class SerializableMisc {
	/** Writes the given object to the given file. */
	public static <T extends Serializable> void write(File file, T object) throws IOException {
		try (ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			output.writeObject(object);
		}
	}

	/** Reads an object from the given file. */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T read(File file) throws ClassNotFoundException, IOException {
		try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			return (T) input.readObject();
		}
	}

	/** Writes an exception to file, even if that exception isn't serializable. */
	public static void writeThrowable(File file, Throwable object) throws IOException {
		try {
			// write the exception as-is
			write(file, object);
		} catch (NotSerializableException e) {
			// if the exception is not serializable, then we'll
			// copy it in a way that is guaranteed to be serializable
			write(file, new ThrowableCopy(object));
		}
	}

	/** Copies an exception hierarchy (class, message, and stacktrace). */
	static class ThrowableCopy extends Throwable {
		private static final long serialVersionUID = -4674520369975786435L;

		ThrowableCopy(Throwable source) {
			super(source.getClass().getName() + ": " + source.getMessage());
			setStackTrace(source.getStackTrace());
			if (source.getCause() != null) {
				initCause(new ThrowableCopy(source.getCause()));
			}
		}
	}
}
