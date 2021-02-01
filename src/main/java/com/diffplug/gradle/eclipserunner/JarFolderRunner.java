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


import com.diffplug.gradle.ConfigMisc;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs an `EclipseApp` within this JVM using a folder containing
 * a `plugins` folder with the necessary jars.
 */
public class JarFolderRunner implements EclipseRunner {

	private static final String CLASSLOADER_APP = "app";
	private static final String CLASSLOADER_EXT = "ext";
	private static final String CLASSLOADER_BOOT = "boot";

	private static final String PROP_PARENT_CLASSLOADER = "osgi.parentClassloader";
	private static final String PROP_FRAMEWORK_PARENT_CLASSLOADER = "osgi.frameworkParentClassloader";

	public static final String PROP_EXTENSIONS = "osgi.framework.extensions";

	final File rootDirectory;

	public JarFolderRunner(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	@Override
	public void run(List<String> args) throws Exception {

		Map<String, String> initProps = new HashMap<>();
		String javaVersion = System.getProperty("java.version");
		ClassLoader parent = null;
		URL[] bootpath = null;
		if (javaVersion != null && JavaVersion.fromString(javaVersion).isGreaterOrEqual("1.9.0")) {
			// Running on Java 9+
			initProps.put(PROP_PARENT_CLASSLOADER, CLASSLOADER_EXT);
			initProps.put(PROP_FRAMEWORK_PARENT_CLASSLOADER, CLASSLOADER_EXT);
			// In J9+ the SystemClassLoader is a AppClassLoader. Thus we need it's parent
			ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
			parent = appClassLoader.getParent();
			bootpath = ClassPathUtil.getClasspath(appClassLoader);
		} else {
			// Running on Java 8
			initProps.put(PROP_PARENT_CLASSLOADER, CLASSLOADER_BOOT);
			initProps.put(PROP_FRAMEWORK_PARENT_CLASSLOADER, CLASSLOADER_BOOT);
			parent = ClassLoader.getSystemClassLoader();
			bootpath = ClassPathUtil.getClasspath(parent);
		}
		final StartupClassLoader classLoader = new StartupClassLoader(bootpath, parent);

		Class<?> installationClazz = classLoader.loadClass("com.diffplug.gradle.eclipserunner.EquinoxInstallation");
		Constructor<?> constructor = installationClazz.getDeclaredConstructor(File.class);
		Object installation = constructor.newInstance(rootDirectory);
		Method addInitProperty = installationClazz.getDeclaredMethod("addInitProperty", String.class, String.class);
		for (Map.Entry<String, String> e : initProps.entrySet()) {
			addInitProperty.invoke(installation, e.getKey(), e.getValue());
		}

		Method getP2Properties = installationClazz.getDeclaredMethod("getP2Properties");
		final Map<String, String> properties = (Map<String, String>) getP2Properties.invoke(installation);
		if (properties.containsKey(PROP_EXTENSIONS)) {
			List<String> frameworkExtensions = ConfigMisc.tokenize(properties.get(PROP_EXTENSIONS), ",");
			for (String extension : frameworkExtensions) {
				ClassPathUtil misc = new ClassPathUtil(rootDirectory.getAbsolutePath());
				String extFile = misc.searchForBundle(extension);
				if (extFile != null) {
					classLoader.addExtensionPath(extFile);
				}
			}
		}
		Class<?> launcherClazz = classLoader.loadClass("com.diffplug.gradle.eclipserunner.EquinoxLauncher");
		constructor = launcherClazz.getConstructor(installationClazz);
		Object launcher = constructor.newInstance(installation);
		Method setArgs = launcherClazz.getDeclaredMethod("setArgs", List.class);
		setArgs.invoke(launcher, args);
		Method run = launcherClazz.getDeclaredMethod("run");
		run.invoke(launcher);
	}
}
