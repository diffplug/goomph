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
package com.diffplug.gradle.eclipserunner.osgiembed;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.SerializableMisc;

/**
 * Runs code that lives outside an OSGi
 * container inside of it.  Works just like
 * {@link com.diffplug.gradle.JavaExecable}.
 */
public interface OsgiExecable extends Serializable, Runnable {
	/** Executes the given {@link OsgiExecable} within an embedded OSGi runtime. */
	public static <T extends OsgiExecable> T exec(BundleContext context, T input) throws Exception {
		// find OsgiExecImp.execInternal within the OSGi runtime
		Bundle bundle = OsgiExecImp.loadBundle(context);
		Class<?> clazz = bundle.loadClass(OsgiExecImp.class.getName());
		Method execInternal = clazz.getMethod("execInternal", File.class);
		// write the input to a tempfile
		File tempFile = File.createTempFile("OsgiExec", ".temp");
		try {
			SerializableMisc.write(tempFile, input);
			// call it within the OSGi runtime
			execInternal.setAccessible(true);
			execInternal.invoke(null, tempFile);
			// get the result, and return it
			return SerializableMisc.read(tempFile);
		} finally {
			FileMisc.delete(tempFile);
		}
	}
}
