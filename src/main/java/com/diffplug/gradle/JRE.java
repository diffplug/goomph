/*
 * Copyright (C) 2021 DiffPlug
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
package com.diffplug.gradle;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JRE {
	/** Returns 8, 9, 10, etc. */
	public static int majorVersion() {
		String jre = System.getProperty("java.version");
		if (jre.startsWith("1.8")) {
			return 8;
		} else {
			Matcher matcher = Pattern.compile("(\\d+)").matcher(jre);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer");
			}
			int version = Integer.parseInt(matcher.group(1));
			if (version <= 8) {
				throw new IllegalArgumentException("Expected " + jre + " to start with an integer greater than 8");
			}
			return version;
		}
	}

	/** Returns the classpath of either a URLClassLoader or a Java9+ AppClassLoader. */
	public static URL[] getClasspath(ClassLoader classLoader) throws Exception {
		URL[] urls;
		if (classLoader instanceof URLClassLoader) {
			urls = ((URLClassLoader) classLoader).getURLs();
		} else {
			// Assume AppClassLoader of Java9+
			Class<? extends ClassLoader> clz = classLoader.getClass();
			Field ucpFld;
			try {
				// Java 9 - 15
				ucpFld = clz.getDeclaredField("ucp");
			} catch (NoSuchFieldException e) {
				// Java 16+
				ucpFld = clz.getSuperclass().getDeclaredField("ucp");
			}
			ucpFld.setAccessible(true);
			Object ucpObj = ucpFld.get(classLoader);
			Field pathFld = ucpObj.getClass().getDeclaredField("path");
			pathFld.setAccessible(true);
			@SuppressWarnings("unchecked")
			List<URL> pathObj = (List<URL>) pathFld.get(ucpObj);
			urls = pathObj.toArray(new URL[pathObj.size()]);
		}
		return extractLongClasspathJar(urls);
	}

	private static URL[] extractLongClasspathJar(URL[] in) throws IOException {
		if (in == null || in.length == 0) {
			return new URL[0];
		}
		File only = new File(in[0].getFile());
		if (in.length == 1 && only.getName().startsWith(JavaExecWinFriendly.LONG_CLASSPATH_JAR_PREFIX)) {
			List<URL> urls = new ArrayList<>();
			try (JarFile file = new JarFile(only)) {
				String cp = file.getManifest().getMainAttributes().getValue("Class-Path");
				for (String entry : cp.split(" ")) {
					urls.add(new URL(entry));
				}
			}
			return urls.toArray(new URL[0]);
		} else {
			return in;
		}
	}
}
