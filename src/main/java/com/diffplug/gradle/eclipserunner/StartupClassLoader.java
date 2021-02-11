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
package com.diffplug.gradle.eclipserunner;


import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;

public class StartupClassLoader extends URLClassLoader {

	private final List<String> extensionPaths = new ArrayList<>();

	public StartupClassLoader(URL[] urls) {
		super(urls);
	}

	public StartupClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public StartupClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
	}

	@Override
	protected String findLibrary(String name) {
		if (extensionPaths.isEmpty()) {
			return super.findLibrary(name);
		}
		String libName = System.mapLibraryName(name);
		for (String extensionPath : extensionPaths) {
			File libFile = new File(extensionPath, libName);
			if (libFile.isFile())
				return libFile.getAbsolutePath();
		}
		return super.findLibrary(name);
	}

	public void addExtensionPath(String path) {
		extensionPaths.add(path);
	}

	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}

	// preparing for Java 9
	protected URL findResource(String moduleName, String name) {
		return findResource(name);
	}

	// preparing for Java 9
	protected Class<?> findClass(String moduleName, String name) {
		try {
			return findClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
