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


import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
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
		ClassLoader parent = null;
		URL[] bootpath = null;
		if (JreVersion.thisVm() >= 9) {
			// In J9+ the SystemClassLoader is a AppClassLoader. Thus we need it's parent
			ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
			parent = appClassLoader.getParent();
			bootpath = ClassPathUtil.getClasspath(appClassLoader);
		} else {
			// Running on Java 8
			parent = ClassLoader.getSystemClassLoader();
			bootpath = ClassPathUtil.getClasspath(parent);
		}
		try (URLClassLoader classLoader = new URLClassLoader(bootpath, parent)) {
			Class<?> launcherClazz = classLoader.loadClass("com.diffplug.gradle.eclipserunner.EquinoxLauncher");
			Object launcher = launcherClazz.getConstructor(File.class).newInstance(rootDirectory);
			launcherClazz.getDeclaredMethod("setArgs", List.class).invoke(launcher, args);
			launcherClazz.getDeclaredMethod("run").invoke(launcher);
		}
	}
}
