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
package com.diffplug.gradle.pde;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.osgi.framework.Version;

import aQute.lib.filter.Filter;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.HashMultimap;
import com.diffplug.common.collect.SetMultimap;
import com.diffplug.common.collect.Sets;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.ZipMisc;

/** Catalogs all of the plugins and their versions in the given paths. */
class PluginCatalog {
	/** A map from plugin name to a set of available versions. */
	private final SetMultimap<String, Version> availableVersions = HashMultimap.create();
	/** A version mapping policy. */
	private final ExplicitVersionPolicy versionPolicy;
	/** A set containing plugins which are specific to platforms which we don't support. */
	private final Set<String> unsupportedPlatform = Sets.newHashSet();

	private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
	private static final String BUNDLE_NAME = "Bundle-SymbolicName";
	private static final String BUNDLE_VERSION = "Bundle-Version";
	private static final String ECLIPSE_PLATFORM_FILTER = "Eclipse-PlatformFilter";

	/**
	 * Catalogs all of the plugins in the given roots.  If a plugin
	 * exists with two versions, an exception is thrown, unless it is
	 * handled by the MultipleVersionPolicy.
	 */
	PluginCatalog(ExplicitVersionPolicy versionPolicy, List<SwtPlatform> supported, List<File> roots) {
		this.versionPolicy = Objects.requireNonNull(versionPolicy);
		for (File root : roots) {
			Preconditions.checkArgument(root.exists(), "Root '%s' does not exist.", root);
			File pluginRoot = root;
			File plugins = new File(root, "plugins");
			if (plugins.exists()) {
				pluginRoot = plugins;
			}

			List<File> files = FileMisc.list(pluginRoot);
			Preconditions.checkArgument(files.size() > 0, "No plugins found in " + root);
			// look for plugin.jar
			files.stream().filter(file -> file.isFile() && file.getName().endsWith(".jar"))
					.forEach(Errors.rethrow().wrap(file -> {
						ZipMisc.read(file, MANIFEST_PATH, input -> addManifest(supported, new Manifest(input)));
					}));
			// look for folder-style plugins (especially org.eclipse.core.runtime.compatibility.registry)
			files.stream().filter(file -> file.isDirectory()).forEach(Errors.rethrow().wrap(file -> {
				File manifestFile = new File(file, MANIFEST_PATH);
				if (manifestFile.exists()) {
					try (FileInputStream input = new FileInputStream(new File(file, MANIFEST_PATH))) {
						addManifest(supported, new Manifest(input));
					}
				}
			}));
		}
	}

	/** Adds a manifest to the catalog. */
	private void addManifest(List<SwtPlatform> supported, Manifest parsed) {
		// parse out the name (looking out for the ";singleton=true" names
		String name = parsed.getMainAttributes().getValue(BUNDLE_NAME);
		int splitIdx = name.indexOf(';');
		if (splitIdx > 0) {
			name = name.substring(0, splitIdx);
		}

		// parse out the platform filter (if any)
		// if it doesn't match an OS that we support, throw it out
		String platformFilter = parsed.getMainAttributes().getValue(ECLIPSE_PLATFORM_FILTER);
		if (platformFilter != null) {
			Filter filter = new Filter(platformFilter.replace(" ", ""));
			boolean isSupportedOS = supported.stream().anyMatch(Errors.rethrow().wrapPredicate(platform -> filter.matchMap(platform.platformProperties())));
			if (!isSupportedOS) {
				unsupportedPlatform.add(name);
				return;
			}
		}

		// parse out the version
		String versionRaw = parsed.getMainAttributes().getValue(BUNDLE_VERSION);
		Version version = Version.parseVersion(versionRaw);
		availableVersions.put(name, version);
	}

	/** Returns true if the given plugin is for a supported platform. */
	boolean isSupportedPlatform(String plugin) {
		return !unsupportedPlatform.contains(plugin);
	}

	/** Returns the version for the given plugin. */
	Set<Version> getVersionsFor(String plugin) {
		return versionPolicy.useVersions(plugin, availableVersions.get(plugin));
	}

	@Override
	public String toString() {
		return availableVersions.entries().stream().map(entry -> entry.toString()).collect(Collectors.joining("\n"));
	}
}
