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


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.SortedSetMultimap;
import com.diffplug.common.collect.TreeMultimap;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.eclipserunner.launcher.Main;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiConsumer;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.Version;

public class EquinoxInstallation {

	final File installationRoot;
	final SortedSetMultimap<String, Version> plugins = TreeMultimap.create();

	public EquinoxInstallation(File installationRoot) {
		this.installationRoot = Objects.requireNonNull(installationRoot);
		// populate the plugins
		File pluginsDir = new File(installationRoot, "plugins");
		Preconditions.checkArgument(FileMisc.dirExists(pluginsDir), "Eclipse launcher must have a plugins directory: %s", installationRoot);
		for (File file : FileMisc.list(pluginsDir)) {
			if (file.isFile()) {
				String fileName = file.getName();
				if (fileName.endsWith(".jar")) {
					int verSplit = fileName.lastIndexOf('_');

					// the name and version of the plugin
					// General scheme is name_version.jar
					// But sometimes name can have underscore: org.eclipse.swt.win32.win32.x86_64_3.104.2.v20160212-1350.jar
					// And sometimes version can hava underscore: org.w3c.dom.events_3.0.0.draft20060413_v201105210656.jar
					// Probably right thing is regex for _#.#.#, but easy thing is this iterative nonsense
					while (verSplit != -1) {
						try {
							String name = fileName.substring(0, verSplit);
							String version = fileName.substring(verSplit + 1, fileName.length() - ".jar".length());
							plugins.put(name, Version.valueOf(version));
							break;
						} catch (IllegalArgumentException e) {
							verSplit = fileName.lastIndexOf('_', verSplit - 1);
						}
					}
				}
			}
		}
		// make sure the plugins we need are present
		BiConsumer<String, String> requireBecause = (name, reason) -> {
			Preconditions.checkArgument(plugins.containsKey(name), "%s is required for %s", name, reason);
		};
		requireBecause.accept("org.eclipse.osgi", "running the OSGi platform");
		requireBecause.accept("org.eclipse.equinox.common", "bundle discovery and installation");
		requireBecause.accept("org.eclipse.update.configurator", "bundle discovery and installation");
		requireBecause.accept("org.eclipse.core.runtime", "eclipse application support");
		requireBecause.accept("org.eclipse.equinox.ds", "OSGi declarative services");

	}

	public File getPluginRequireSingle(String name) {
		SortedSet<Version> versions = plugins.get(name);
		Preconditions.checkArgument(versions.size() == 1, "Expected 1 version for %s, had %s", name, versions);
		String version = Iterables.getOnlyElement(versions).toString();
		return installationRoot.toPath().resolve("plugins/" + name + "_" + version + ".jar").toFile();
	}

	public Map<String, String> getInitProperties() {
		Map<String, String> initProperties = new HashMap<>();
		initProperties.put("osgi.framework.useSystemProperties", "false");
		initProperties.put(EclipseStarter.PROP_INSTALL_AREA, installationRoot.getAbsolutePath());
		initProperties.put(EclipseStarter.PROP_NOSHUTDOWN, "false");
		initProperties.put(EclipseStarter.PROP_FRAMEWORK, getPluginRequireSingle("org.eclipse.osgi").toURI().toString());
		String classLoaderKind = JreVersion.thisVm() >= 9 ? Main.PARENT_CLASSLOADER_EXT : Main.PARENT_CLASSLOADER_BOOT;
		initProperties.put(Main.PROP_PARENT_CLASSLOADER, classLoaderKind);
		initProperties.put(Main.PROP_FRAMEWORK_PARENT_CLASSLOADER, classLoaderKind);
		return initProperties;
	}
}
