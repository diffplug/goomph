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
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.osgi.framework.Version;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;

/** Models the "product" part of {@link PdeBuildTask}. */
public class PdeProductBuildConfig {
	private final Project project;

	public PdeProductBuildConfig(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	String id;
	Object productPluginDir;
	String productFileWithinPlugin;
	String version;

	public void id(String id) {
		this.id = id;
	}

	public void productPluginDir(Object productPluginDir) {
		this.productPluginDir = productPluginDir;
	}

	public void productFileWithinPlugin(String productFileWithinPlugin) {
		this.productFileWithinPlugin = productFileWithinPlugin;
	}

	public void version(String version) {
		this.version = version;
	}

	private Action<ExplicitVersionPolicy> explicitVersionPolicy = null;

	public void explicitVersionPolicy(Action<ExplicitVersionPolicy> explicitVersionPolicy) {
		this.explicitVersionPolicy = explicitVersionPolicy;
	}

	void setup(File destinationDir, PdeBuildProperties props, List<SwtPlatform> platforms, List<File> pluginPaths) throws IOException {
		// make sure every required entry was set
		Objects.requireNonNull(id, "Must set `id`");
		Objects.requireNonNull(productPluginDir, "Must set `productPluginDir`");
		Objects.requireNonNull(productFileWithinPlugin, "Must set `productFileWithinPlugin`");
		Objects.requireNonNull(version, "Must set `version`");
		if (pluginPaths.isEmpty()) {
			throw new IllegalArgumentException("There should be at least one pluginPath");
		}

		// create a PluginCatalog and validate the version policy / pluginPaths
		ExplicitVersionPolicy versionPolicy = new ExplicitVersionPolicy();
		if (explicitVersionPolicy != null) {
			explicitVersionPolicy.execute(versionPolicy);
		}
		PluginCatalog catalog = new PluginCatalog(versionPolicy, platforms, pluginPaths);

		// create a fake folder which will contain our sanitized product file
		File productPluginDir = project.file(this.productPluginDir);
		File tempProductDir = new File(destinationDir, productPluginDir.getName());

		// copy all images from original to the sanitized
		copyImages(productPluginDir, tempProductDir);

		// now create the sanitized product file
		File productFile = productPluginDir.toPath().resolve(productFileWithinPlugin).toFile();
		File tempProductFile = tempProductDir.toPath().resolve(productFileWithinPlugin).toFile();
		transformProductFile(productFile, tempProductFile, catalog, version);

		// finally setup the PdeBuildProperties to our temp product stuff
		props.setProp("topLevelElementType", "product");
		props.setProp("topLevelElementId", id);
		props.setProp("product", "/" + tempProductDir.getName() + "/" + productFileWithinPlugin);
	}

	void copyImages(File sourceDir, File destDir) throws IOException {
		// copy all icon files from the original into the temp
		FileFilter filter = file -> {
			return file.isDirectory() ||
					POSSIBLE_ICON_SUFFIXES.stream().anyMatch(ending -> file.getName().endsWith(ending));
		};
		boolean preserveFileDate = false;
		FileUtils.copyDirectory(sourceDir, destDir, filter, preserveFileDate);
	}

	static final ImmutableList<String> POSSIBLE_ICON_SUFFIXES = ImmutableList.of(".xpm", ".icns", ".ico");

	static void transformProductFile(File input, File output, PluginCatalog catalog, String version) throws IOException {
		String inputStr = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
		String result = StringPrinter.buildString(printer -> {
			String[] lines = FileMisc.toUnixNewline(inputStr).split("\n");
			for (String line : lines) {
				transformProductFile(printer, line, catalog, version);
			}
		});
		Files.write(output.toPath(), result.getBytes(StandardCharsets.UTF_8));
	}

	private static final String PLUGIN_PREFIX = "<plugin id=\"";
	private static final String PLUGIN_MIDDLE = "\"";
	private static final String PLUGIN_SUFFIX = "/>";
	private static final String GROUP = "(.*)";
	private static final String NO_QUOTE_GROUP = "([^\"]*)";

	private static final Pattern PLUGIN_REGEX = Pattern.compile(GROUP + PLUGIN_PREFIX + NO_QUOTE_GROUP + PLUGIN_MIDDLE + GROUP + PLUGIN_SUFFIX);

	private static final Pattern PRODUCT_VERSION_REGEX = Pattern.compile("<product (?:.*) version=\"([^\"]*)\"(?:.*)>");
	private static final String VERSION_EQ = "version=";

	static void transformProductFile(StringPrinter printer, String line, PluginCatalog catalog, String version) {
		// if we found the product tag, replace the version with our version
		Matcher productMatcher = PRODUCT_VERSION_REGEX.matcher(line);
		if (productMatcher.matches()) {
			int start = productMatcher.start(1);
			int end = productMatcher.end(1);
			printer.println(line.substring(0, start) + version + line.substring(end));
			return;
		}

		// if it isn't a plugin line, pass it through unscathed
		if (!line.contains("plugin") || line.contains("plugins")) {
			printer.println(line);
			return;
		}
		// if it's a plugin line, and it specifies a version, wipe out the version
		if (line.contains(VERSION_EQ)) {
			System.err.println("Ignoring version in " + line + ", Goomph sets it automatically.");
			int versionStart = line.indexOf(VERSION_EQ);
			int versionEnd = line.indexOf('"', versionStart + VERSION_EQ.length() + 1);
			line = line.substring(0, versionStart) + line.substring(versionEnd + 1);
		}

		Matcher pluginMatcher = PLUGIN_REGEX.matcher(line);
		if (pluginMatcher.matches()) {
			String pluginName = pluginMatcher.group(2);
			if (!catalog.isSupportedPlatform(pluginName)) {
				// ignore plugins for unsupported platforms
				return;
			} else {
				// set versions for all the rest
				for (Version pluginVersion : catalog.getVersionsFor(pluginName)) {
					printer.println(pluginMatcher.group(1) + PLUGIN_PREFIX + pluginName + PLUGIN_MIDDLE + " version=\"" + pluginVersion + "\"" + pluginMatcher.group(3) + PLUGIN_SUFFIX);
				}
			}
		} else {
			System.err.println("Unexpected line " + line);
		}
	}
}
