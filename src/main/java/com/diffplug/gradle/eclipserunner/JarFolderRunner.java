/*
 * Copyright (C) 2015-2021 DiffPlug
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


import com.diffplug.gradle.JRE;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs an `EclipseApp` within this JVM using a folder containing
 * a `plugins` folder with the necessary jars.
 */
public class JarFolderRunner implements EclipseRunner {
	final File rootDirectory;

	public JarFolderRunner(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	@Override
	public void run(List<String> args) throws Exception {
		File plugins = new File(rootDirectory, "plugins");
		List<URL> osgiClasspath = new ArrayList<>();
		populateClasspath(plugins, osgiClasspath);
		try (URLClassLoader classLoader = open(osgiClasspath)) {
			Class<?> launcherClazz = classLoader.loadClass("com.diffplug.gradle.eclipserunner.EquinoxLauncher");
			Object launcher = launcherClazz.getConstructor(File.class).newInstance(rootDirectory);
			launcherClazz.getDeclaredMethod("setArgs", List.class).invoke(launcher, args);
			launcherClazz.getDeclaredMethod("run").invoke(launcher);
		}
	}

	private void populateClasspath(File dirToExplore, List<URL> osgiClasspath) throws MalformedURLException {
		for (File file : dirToExplore.listFiles()) {
			if (file.isFile() && file.getName().endsWith(".jar")) {
				System.out.println("add " + file.getName());
				osgiClasspath.add(file.toURI().toURL());
			} else if (file.isDirectory()) {
				populateClasspath(file, osgiClasspath);
			}
		}
	}

	public static URLClassLoader open(List<URL> urls) throws Exception {
		ClassLoader parent = null;
		URL[] boot;
		if (JRE.majorVersion() >= 9) {
			// In J9+ the SystemClassLoader is a AppClassLoader. Thus we need it's parent
			ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
			parent = appClassLoader.getParent();
			boot = JRE.getClasspath(appClassLoader);
		} else {
			// Running on Java 8
			parent = ClassLoader.getSystemClassLoader();
			boot = JRE.getClasspath(parent);
		}
		List<URL> classpath = new ArrayList<>();
		classpath.addAll(urls);
		classpath.addAll(Arrays.asList(boot));
		return new URLClassLoader(classpath.toArray(new URL[0]), parent);
	}
}
