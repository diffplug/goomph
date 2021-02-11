/*
 * Copyright (C) 2016-2021 DiffPlug
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

import static java.util.stream.Collectors.toList;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.osgi.framework.BundleContext;

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

	private final EquinoxInstallation installation;

	/**
	 * Wraps a directory of jars in the launcher API, and
	 * ensures the the directory contains the plugins required
	 * to run a barebones equinox instance.
	 */
	public EquinoxLauncher(EquinoxInstallation installation) {
		this.installation = installation;
	}

	/** Returns the plugin file for the given name, ensuring that there is exactly one version available. */
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
	 * Available options [here](https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Fruntime-options.html).
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
			Map<String, String> defaults = installation.getInitProperties();
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
