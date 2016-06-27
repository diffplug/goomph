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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

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

	/**
	 * Defines data which will be passed  reflection to code within the OSGi runtime - the reflection
	 * allows us to call code for which we don't have the necessary dependencies to resolve its imports
	 * unless it is only instantiated within the OSGi container.
	 */
	@SuppressWarnings("serial")
	public static abstract class ReflectionHost implements OsgiExecable {
		private final String delegate;

		protected ReflectionHost(String delegate) {
			this.delegate = Objects.requireNonNull(delegate);
		}

		@Override
		public void run() {
			try {
				Bundle bundle = FrameworkUtil.getBundle(OsgiExecable.class);
				Class<?> clazz = bundle.loadClass(delegate);
				Constructor<?>[] constructors = clazz.getDeclaredConstructors();
				if (constructors.length != 1) {
					throw new IllegalArgumentException(delegate + " must have only one constructor, this had " + constructors.length);
				}
				if (constructors[0].getParameterCount() != 1) {
					throw new IllegalArgumentException(delegate + "'s constructor must take one argument, this took " + constructors[0].getParameterCount());
				}
				constructors[0].setAccessible(true);
				ReflectionClient<?> client = (ReflectionClient<?>) (Object) constructors[0].newInstance(this);
				client.run();
			} catch (SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/** Client code which gets called within the OSGi runtime. */
	public static abstract class ReflectionClient<Host extends ReflectionHost> implements Runnable {
		protected final Host host;

		protected ReflectionClient(Host host) {
			this.host = Objects.requireNonNull(host);
		}
	}
}
