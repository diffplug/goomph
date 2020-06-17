/*
 * Copyright (C) 2016-2019 DiffPlug
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
package com.diffplug.gradle.pde;


import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.FileMisc;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.Version;

public class ProductFileUtil {
	private static final String PLUGIN_PREFIX = "<plugin id=\"";
	private static final String PLUGIN_MIDDLE = "\"";
	private static final String PLUGIN_SUFFIX = "/>";
	private static final String GROUP = "(.*)";
	private static final String NO_QUOTE_GROUP = "([^\"]*)";

	private static final Pattern PLUGIN_REGEX = Pattern.compile(GROUP + PLUGIN_PREFIX + NO_QUOTE_GROUP + PLUGIN_MIDDLE + GROUP + PLUGIN_SUFFIX);

	private static final Pattern PRODUCT_VERSION_REGEX = Pattern.compile("<product (?:.*) version=\"([^\"]*)\"(?:.*)>");
	private static final Pattern INCLUDE_LAUNCHER_REGEX = Pattern.compile("<product (?:.*) includeLaunchers=\"([^\"]*)\"(?:.*)>");
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

	static Optional<String> parsePlugin(String line) {
		// if it isn't a plugin line, pass it through unscathed
		if (!line.contains("plugin") || line.contains("plugins")) {
			return Optional.empty();
		}
		Matcher pluginMatcher = PLUGIN_REGEX.matcher(line);
		if (pluginMatcher.matches()) {
			String pluginName = pluginMatcher.group(2);
			return Optional.of(pluginName);
		} else {
			return Optional.empty();
		}
	}

	static Map<String, String> extractProperties(String[] lines) {
		Map<String, String> props = new LinkedHashMap<>();
		for (String line : lines) {
			Matcher includeLauncherMatcher = INCLUDE_LAUNCHER_REGEX.matcher(line);
			if (includeLauncherMatcher.matches()) {
				props.put("includeLaunchers", includeLauncherMatcher.group(1));
			}
		}
		return props;
	}

	static String[] readLines(File productFile) throws IOException {
		String inputStr = new String(Files.readAllBytes(productFile.toPath()), StandardCharsets.UTF_8);
		return FileMisc.toUnixNewline(inputStr).split("\n");
	}
}
