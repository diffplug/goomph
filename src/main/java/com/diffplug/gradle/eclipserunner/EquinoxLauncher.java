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
package com.diffplug.gradle.eclipserunner;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.SortedSetMultimap;
import com.diffplug.common.collect.TreeMultimap;
import com.diffplug.gradle.FileMisc;

/**
 * Given a directory containing osgi jars, this class
 * verifies that the core bundles are available, and
 * provides an API for instantiating the OSGi runtime
 * and accessing its {@link BundleContext}.
 *
 * See {@link #setProps(Map)} for precise details on the
 * default framework properties.
 */
public class EquinoxLauncher {
	final File installationRoot;
	final SortedSetMultimap<String, Version> plugins = TreeMultimap.create();

	/**
	 * Wraps a directory of jars in the launcher API, and
	 * ensures the the directory contains the plugins required
	 * to run a barebones equinox instance.
	 */
	public EquinoxLauncher(File installationRoot) {
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

	/** Returns the plugin file for the given name, ensuring that there is exactly one version available. */
	private File getPluginRequireSingle(String name) {
		SortedSet<Version> versions = plugins.get(name);
		Preconditions.checkArgument(versions.size() == 1, "Expected 1 version for %s, had %s", name, versions);
		String version = Iterables.getOnlyElement(versions).toString();
		return installationRoot.toPath().resolve("plugins/" + name + "_" + version + ".jar").toFile();
	}

	ImmutableList<String> args = ImmutableList.of();
	ImmutableMap<String, String> props = ImmutableMap.of();

	/** Sets the application arguments which will be passed to the runtime. */
	public EquinoxLauncher setArgs(List<String> args) {
		// Filter --launcher.suppressErrors
		// Filter --nosplash
		List<String> filteredArgs = args.stream()
				.filter(Objects::nonNull)
				.filter(arg -> !arg.equals("--launcher.suppressErrors"))
				.filter(arg -> !arg.equals("-nosplash"))
				.collect(toList());
		this.args = ImmutableList.copyOf(filteredArgs);
		return this;
	}

	/**
	 * Sets the system properties which will be set on the runtime.
	 *
	 * Available options [here](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fruntime-options.html).
	 *
	 * This should usually not need to be set.  Below are the default properties.  To unset
	 * one of the defaults, set its value to the empty string and it will be cleared.
	 *
	 * ```java
	 * map.put("osgi.framework.useSystemProperties", "false");
	 * map.put("osgi.install.area", <installation root>);
	 * map.put("osgi.noShutdown", "false");
	 * // enable 
	 * map.put("equinox.use.ds", "true");
	 * map.put("osgi.bundles", Joiner.on(", ").join(
	 *     // automatic bundle discovery and installation
	 *     "org.eclipse.equinox.common@2:start",
	 *     "org.eclipse.update.configurator@3:start",
	 *     // support eclipse's -application argument
	 *     "org.eclipse.core.runtime@4:start",
	 *     // declarative services
	 *     "org.eclipse.equinox.ds@5:start"));
	 * 	map.put(EclipseStarter.PROP_FRAMEWORK, <path to plugin "org.eclipse.osgi">);
	 * ```
	 */
	public EquinoxLauncher setProps(Map<String, String> props) {
		this.props = ImmutableMap.copyOf(props);
		return this;
	}

	/**
	 * Opens the eclipse runtime, and returns an instance of
	 * {@link Running} which allows access to the underlying
	 * {@link BundleContext}.
	 */
	public Running open() throws Exception {
		return new Running(props, args);
	}

	/** Runs the equinox launcher (calls {@link #open()} and immediately closes it). */
	public void run() throws Exception {
		try (Running running = open()) {
			running.run();
		}
	}

	/**
	 * Represents a running instance of the equinox
	 * OSGi container.  Shuts down the container when
	 * you call {@link #close()}.
	 */
	public class Running implements AutoCloseable {
		final BundleContext bundleContext;

		private Running(Map<String, String> systemProps, List<String> args) throws Exception {
			Map<String, String> defaults = defaultSystemProperties();
			modifyDefaultBy(defaults, systemProps);
			EclipseStarter.setInitialProperties(defaults);
			bundleContext = EclipseStarter.startup(args.toArray(new String[0]), null);
			Objects.requireNonNull(bundleContext);
		}

		/** The {@link BundleContext} of the running eclipse instance. */
		public BundleContext bundleContext() {
			return bundleContext;
		}

		/** Runs an eclipse application, as specified by the `-application` argument. */
		private void run() throws Exception {
			EclipseStarter.run(null);
			
			// request now, after shutdown the bundleContext cannot be queried
			BundleContext bundleContext = EclipseStarter.getSystemBundleContext();
			
			// wait for the termination of the application
			// this needed if the application does not do all its work in the IApplication#start
			// and sets the exit code asynchronously
			EclipseStarter.shutdown();

			String result = bundleContext.getProperty(EclipseStarter.PROP_EXITCODE);
			
			Preconditions.checkState("0".equals(result), "Unexpected return=0, was: %s", result);
		}

		/** Shutsdown the eclipse instance. */
		@Override
		public void close() throws Exception {
			EclipseStarter.shutdown();
		}
	}

	private Map<String, String> defaultSystemProperties() {
		Map<String, String> map = new HashMap<>();
		map.put("osgi.framework.useSystemProperties", "false");
		map.put(EclipseStarter.PROP_INSTALL_AREA, installationRoot.getAbsolutePath());
		map.put(EclipseStarter.PROP_NOSHUTDOWN, "false");
		// enable 
		map.put("equinox.use.ds", "true");
		map.put(EclipseStarter.PROP_BUNDLES, Joiner.on(", ").join(
				// automatic bundle discovery and installation
				"org.eclipse.equinox.common@2:start",
				"org.eclipse.update.configurator@3:start",
				// support eclipse's -application argument
				"org.eclipse.core.runtime@4:start",
				// declarative services
				"org.eclipse.equinox.ds@5:start"));
		map.put(EclipseStarter.PROP_FRAMEWORK, getPluginRequireSingle("org.eclipse.osgi").toURI().toString());
		return map;
	}

	private void modifyDefaultBy(Map<String, String> defaultMap, Map<String, String> modify) {
		modify.forEach((key, value) -> {
			if (value.isEmpty()) {
				defaultMap.remove(key);
			} else {
				defaultMap.put(key, value);
			}
		});
	}
}
