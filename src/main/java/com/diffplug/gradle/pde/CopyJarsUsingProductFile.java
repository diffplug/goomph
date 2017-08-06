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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.osgi.framework.Version;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.Lazyable;

/**
 * THIS CLASS IS EXPERIMENTAL AND SUBJECT TO CHANGE.
 * 
 * Copies jars into a folder, using a product file and an
 * {@link ExplicitVersionPolicy} to decide which jars to copy.
 * Useful for passing jars to analysis tools.
 *
 * ```groovy
 * task copyJars(type: CopyJarsUsingProductFile) {
 *     // uses jars from the given folders / installations
 *     from TARGETMAVEN_PLUGINS_DIR
 *     from TARGETP2_RUNNABLE_DIR
 *     // uses the given product file to determine which jars to copy
 *     productFile rootProject.file('com.diffplug.rcpdemo/rcpdemo.product')
 *     // adds plugins which aren't included by the productFile
 *     extra('org.jsr-305')
 *     // destination for the copied jars
 *     into COPY_PRODUCT_JARS
 *     // determines which version to use when multiple versions are available
 *     explicitVersionPolicy {
 *         resolve('com.jcraft.jsch', '0.1.53.autowrapped', '0.1.53.v201508180515').withFirst()
 *         resolve('org.apache.commons.codec', '1.6.0', '1.6.0.v201305230611').withFirst()
 *         resolve('org.apache.commons.logging', '1.1.3', '1.1.1.v201101211721').withFirst()
 *         resolve('org.apache.httpcomponents.httpclient', '4.3.6.autowrapped', '4.3.6.v201411290715').withFirst()
 *         resolve('org.apache.httpcomponents.httpcore', '4.3.3.autowrapped', '4.3.3.v201411290715').withFirst()
 *         resolve('org.hamcrest.core', '1.3.0.autowrapped', '1.3.0.v201303031735').withFirst()
 *         resolve('org.tukaani.xz', '1.4.0', '1.3.0.v201308270617').withFirst()
 *     }
 * }
 * ```
 */
public class CopyJarsUsingProductFile extends DefaultTask {
	private Lazyable<ExplicitVersionPolicy> explicitVersionPolicy = ExplicitVersionPolicy.createLazyable();

	private List<File> inputFolders = new ArrayList<>();

	private File productFile;

	private File destination;

	public void from(Object from) {
		inputFolders.add(getProject().file(from));
		getInputs().dir(from);
	}

	public void productFile(Object file) {
		productFile = getProject().file(file);
		getInputs().file(productFile);
	}

	public void explicitVersionPolicy(Action<ExplicitVersionPolicy> action) {
		explicitVersionPolicy.addLazyAction(action);
	}

	public void into(Object dest) {
		this.destination = getProject().file(dest);
		getOutputs().dir(dest);
	}

	List<String> extras = new ArrayList<>();

	public void extra(String extra) {
		extras.add(extra);
	}

	@TaskAction
	public void action() throws IOException {
		Objects.requireNonNull(explicitVersionPolicy, "Set explicitVersionPolicy");
		Objects.requireNonNull(destination, "Set destination");
		Preconditions.checkArgument(!inputFolders.isEmpty(), "Input folders should not be empty");

		FileMisc.cleanDir(destination);

		PluginCatalog catalog = new PluginCatalog(explicitVersionPolicy.getResult(), SwtPlatform.getAll(), inputFolders);
		String inputStr = new String(Files.readAllBytes(productFile.toPath()), StandardCharsets.UTF_8);
		String[] lines = FileMisc.toUnixNewline(inputStr).split("\n");
		for (String line : lines) {
			ProductFileUtil.parsePlugin(line).ifPresent(plugin -> copyVersionsOfPlugin(catalog, plugin));
		}
		extras.forEach(plugin -> copyVersionsOfPlugin(catalog, plugin));
	}

	private void copyVersionsOfPlugin(PluginCatalog catalog, String plugin) {
		if (!catalog.isSupportedPlatform(plugin)) {
			return;
		}
		Set<Version> versions = catalog.getVersionsFor(plugin);
		Preconditions.checkNotNull(versions, "No versions available for %s", plugin);
		for (Version version : versions) {
			File source = catalog.getFile(plugin, version);
			File dest = new File(destination, source.getName());
			Errors.rethrow().run(() -> {
				if (source.isFile()) {
					FileUtils.copyFile(source, dest);
				} else {
					FileUtils.copyDirectory(source, dest);
				}
			});
		}
	}
}
