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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.collect.Maps;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.ZipUtil;

/**
 * Runs PDE build to make an RCP application or a P2 repository.
 * 
 * WARNING: This part of Goomph currently has the following precondition:
 * your project must have the property VER_ECLIPSE=4.5.2 (or some other version),
 * and you must have installed that Eclipse using Wuff. We will remove this
 * restriction in the future.
 * 
 * ```groovy
 * import com.diffplug.gradle.*
 * import com.diffplug.gradle.pde.*
 * import com.diffplug.common.swt.os.*

 * task diffplugP2(type: PdeProductBuildTask) {
 *     // the directory which will contain the results of the build
 *     buildDir(P2_BUILD_DIR)
 *     // copy the product file and its referenced images
 *     copyProductAndImgs('../com.diffplug.core', 'plugins/com.diffplug.core')
 *     // set the plugins to be the delta pack (implicit)
 *     // and the combined targetplatform / obfuscation result
 *     setPluginPath(PDE_PREP_DIR)
 *     // if multiple versions of a plugin are detected between the pluginPath / targetplatform,
 *     // you must list the plugin name, and all versions which are available.  only the first
 *     // plugin will be included in the final product build
 *     resolveWithFirst('org.apache.commons.codec', '1.9.0', '1.6.0.v201305230611')
 *     resolveWithFirst('org.apache.commons.logging', '1.2.0', '1.1.1.v201101211721')
 *     // set the build properties to be as shown
 *     addBuildProperty('topLevelElementType', 'product')
 *     addBuildProperty('topLevelElementId',   APP_ID)
 *     addBuildProperty('product', '/com.diffplug.core/' + APP_ID)
 *     addBuildProperty('runPackager', 'false')
 *     addBuildProperty('groupConfigurations',     'true')
 *     addBuildProperty('filteredDependencyCheck', 'true')
 *     addBuildProperty('resolution.devMode',      'true')
 *     // configure some P2 pieces
 *     addBuildProperty('p2.build.repo',   'file:' + project.file(P2_REPO_DIR).absolutePath)
 *     addBuildProperty('p2.gathering',    'true')
 *     addBuildProperty('skipDirector',    'true')

 *     // configure gradle's staleness detector
 *     inputs.dir(PDE_PREP_DIR)
 *     outputs.dir(P2_REPO_DIR)

 *     doLast {
 *         // artifact compression reduces content.xml from ~1MB to ~100kB
 *         def compressXml = { name ->
 *             def xml = project.file(P2_REPO_DIR + "/${name}.xml")
 *             def jar = project.file(P2_REPO_DIR + "/${name}.jar")
 *             ZipUtil.zip(xml, "${name}.xml", jar)
 *             xml.delete()
 *         }
 *         compressXml('artifacts')
 *         compressXml('content')
 *     }
 * }
 * ```
 */
public class PdeProductBuildTask extends DefaultTask {
	private Object buildDir;

	/** The directory from which plugins will be pulled, besides the delta pack. */
	public void buildDir(Object buildDir) {
		this.buildDir = buildDir;
	}

	@OutputDirectory
	protected File getBuildDir() {
		return getProject().file(buildDir);
	}

	/** The name of the product file. */
	public void productFilename(String productFile) {
		this.productFilename = productFile;
	}

	private String productFilename;

	private Object pluginPath;

	/** The directory from which plugins will be pulled, besides the delta pack. */
	public void setPluginPath(Object pluginPath) {
		this.pluginPath = pluginPath;
	}

	protected File getPluginPath() {
		return getProject().file(pluginPath);
	}

	private Object featureFolder;

	/** The directory from which plugins will be pulled, besides the delta pack. */
	public void setFeatureFolder(Object featureFolder) {
		this.featureFolder = featureFolder;
	}

	private Map<String, String> buildProperties = Maps.newLinkedHashMap();

	/** Adds a property to the build properties file. */
	public void addBuildProperty(String key, String value) {
		buildProperties.put(key, value);
	}

	private Map<String, List<String>> resolveMap = Maps.newHashMap();

	/** Resolves conflicts at the given plugin with the given versions. */
	public void resolveWithFirst(String pluginName, String... versions) {
		resolveMap.put(pluginName, Arrays.asList(versions));
	}

	private Object copyProductDir;
	private String dstRelPath;

	/** Copies the product and imgs from the given directory to the given path within the build directory. */
	public void copyProductAndImgs(Object src, String dstRelPath) {
		this.copyProductDir = src;
		this.dstRelPath = dstRelPath;
	}

	@TaskAction
	public void build() throws Exception {
		Preconditions.checkNotNull(buildDir, "buildDir must not be null!");

		// delete the buildDir and make a fresh directory
		File buildDir = getBuildDir();
		FileMisc.cleanDir(buildDir);

		// setup build.properties
		PdeProductBuildProperties properties = new PdeProductBuildProperties(getProject());
		properties.setConfigs(SwtPlatform.getAll()); // for all configs
		properties.setBuildDirectory(getBuildDir()); // tied to the appropriate build directory
		if (pluginPath != null) {
			properties.setPluginPath(getPluginPath());
		} else {
			properties.setPluginPath();
		}
		// set all the properties we'd like to set
		for (Map.Entry<String, String> entry : buildProperties.entrySet()) {
			properties.setProp(entry.getKey(), entry.getValue());
		}
		// write build.properties to the appropriate directory
		File buildDirProperties = new File(buildDir, "build.properties");
		Files.write(properties.getContent(), buildDirProperties, StandardCharsets.UTF_8);

		// copy any other files the user might like us to copy
		if (copyProductDir != null) {
			File srcDir = getProject().file(copyProductDir);
			File dstDir = buildDir.toPath().resolve(dstRelPath).toFile();

			// catalog all of the available plugins, and resolve any plugins that require resolving
			PluginCatalog catalog = new PluginCatalog(properties.getPluginLookupPath());
			resolveMap.entrySet().forEach(entry -> {
				catalog.resolveWithFirst(entry.getKey(), entry.getValue());
			});

			// get DiffPlug's version
			String dpVersion = (String) getProject().getProperties().get("VER_DIFFPLUG");

			// set all the version tags in the product file
			File productInput = new File(srcDir, productFilename);
			File productOutput = new File(dstDir, productFilename);

			// read the lines of the file, and set the exact versions
			List<String> lines = Files.readLines(productInput, StandardCharsets.UTF_8);
			setProductFileVersions(productInput, dpVersion, lines, catalog);
			// write it to its destination
			FileMisc.mkdirs(productOutput.getParentFile());
			Files.write(Joiner.on("\n").join(lines), productOutput, StandardCharsets.UTF_8);

			// replace the product file in the plugin in the pluginPath
			File corePlugin = new File(getPluginPath(), "com.diffplug.core_" + dpVersion + ".jar");
			File temp = File.createTempFile("tempPlugin", ".jar");
			ZipUtil.modify(corePlugin, temp, ImmutableMap.of(productFilename, productOutput), Collections.emptySet());
			Files.copy(temp, corePlugin);
			FileMisc.delete(temp);

			// copy only the images needed by the product file
			ImmutableList<String> endingsToCopy = ImmutableList.of(".xpm", ".icns", ".ico");
			FileFilter filter = file -> {
				return file.isDirectory() ||
						endingsToCopy.stream().anyMatch(ending -> file.getName().endsWith(ending));
			};
			boolean preserveFileDate = false;
			FileUtils.copyDirectory(srcDir, dstDir, filter, preserveFileDate);
		}
		if (featureFolder != null) {
			// the folder that holds the feature
			File feature = getProject().file(featureFolder);
			// copy it into the features/featureId folder
			String featureId = feature.getName();
			FileUtils.copyDirectory(feature, buildDir.toPath().resolve("features/" + featureId).toFile());
		}

		// generate and execute the PDE build command
		PdeInstallation installation = PdeInstallation.fromProject(getProject());
		installation.productBuildCmd(buildDir).runUsing(installation);
	}

	private static final String PLUGIN_PREFIX = "<plugin id=\"";
	private static final String PLUGIN_MIDDLE = "\"";
	private static final String PLUGIN_SUFFIX = "/>";
	private static final String GROUP = "(.*)";
	private static final String NO_QUOTE_GROUP = "([^\"]*)";

	private static final Pattern PLUGIN_REGEX = Pattern.compile(GROUP + PLUGIN_PREFIX + NO_QUOTE_GROUP + PLUGIN_MIDDLE + GROUP + PLUGIN_SUFFIX);

	private static final Pattern VERSION_REGEX = Pattern.compile("<product (?:.*) version=\"([^\"]*)\"(?:.*)>");

	/**
	 * Given a list of lines from the product file, this method changes them
	 * in-place to specify the specific version from the plugin path.
	 */
	public static void setProductFileVersions(File productFile, String version, List<String> lines, PluginCatalog catalog) {
		ListIterator<String> iter = lines.listIterator();

		Consumer<String> reportError = errorMsg -> {
			throw new IllegalArgumentException(productFile.getAbsolutePath() + ":" + (iter.previousIndex() + 1) + " " + iter.previous() + "\n" + errorMsg);
		};
		while (iter.hasNext()) {
			String line = iter.next();

			// if we found the product tag, replace the version with our version
			Matcher productMatcher = VERSION_REGEX.matcher(line);
			if (productMatcher.matches()) {
				int start = productMatcher.start(1);
				int end = productMatcher.end(1);
				iter.set(line.substring(0, start) + version + line.substring(end));
			}

			// if it isn't a plugin line, we'll bail
			if (!line.contains("plugin") || line.contains("plugins")) {
				continue;
			}
			if (line.contains("version=")) {
				reportError.accept("Plugins must not contain a version!  We're gonna add the version ourselves.");
			}

			Matcher pluginMatcher = PLUGIN_REGEX.matcher(line);
			if (pluginMatcher.matches()) {
				String pluginName = pluginMatcher.group(2);
				if (!catalog.isSupportedPlatform(pluginName)) {
					// remove plugins for unsupported platforms
					iter.remove();
				} else {
					// set versions for all the rest
					line = pluginMatcher.group(1) + PLUGIN_PREFIX + pluginName + PLUGIN_MIDDLE + " version=\"" + catalog.getVersionFor(pluginName) + "\"" + pluginMatcher.group(3) + PLUGIN_SUFFIX;
					iter.set(line);
				}
			} else {
				reportError.accept("Unexpected line");
			}
		}
	}
}
