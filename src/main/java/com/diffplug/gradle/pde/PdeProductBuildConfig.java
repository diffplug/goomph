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

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.Lazyable;

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
	private String[] productFileLines;
	final Lazyable<ExplicitVersionPolicy> explicitVersionPolicy = ExplicitVersionPolicy.createLazyable();

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

	public void explicitVersionPolicy(Action<ExplicitVersionPolicy> action) {
		explicitVersionPolicy.addLazyAction(action);
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
		PluginCatalog catalog = new PluginCatalog(explicitVersionPolicy.getResult(), platforms, pluginPaths);

		// create a fake folder which will contain our sanitized product file
		File productPluginDir = project.file(this.productPluginDir);
		File tempProductDir = new File(destinationDir, productPluginDir.getName());

		// copy all images from original to the sanitized
		copyImages(productPluginDir, tempProductDir);

		// now create the sanitized product file
		File productFile = productPluginDir.toPath().resolve(productFileWithinPlugin).toFile();
		productFileLines = ProductFileUtil.readLines(productFile);
		ProductFileUtil.extractProperties(productFileLines).forEach((key, value) -> props.setProp(key.toString(), value.toString()));

		File tempProductFile = tempProductDir.toPath().resolve(productFileWithinPlugin).toFile();
		transformProductFile(tempProductFile, catalog, version);

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

	void transformProductFile(File output, PluginCatalog catalog, String version) throws IOException {
		String result = StringPrinter.buildString(printer -> {
			for (String line : productFileLines) {
				ProductFileUtil.transformProductFile(printer, line, catalog, version);
			}
		});
		Files.write(output.toPath(), result.getBytes(StandardCharsets.UTF_8));
	}
}
