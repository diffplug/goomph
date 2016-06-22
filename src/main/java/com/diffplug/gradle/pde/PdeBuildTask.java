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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.collect.Maps;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;

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
public class PdeBuildTask extends DefaultTask {
	private Object destination;

	/** Sets the target directory. */
	public void destination(Object buildDir) {
		this.destination = buildDir;
	}

	private Object base;

	/**
	 * Sets the directory which contains either the
	 * extracted delta pack, or the result of a p2
	 * mirror task containing something like this:
	 *
	 * ```groovy
	 * p2.addFeature('org.eclipse.equinox.executable')
	 * p2.addFeature('org.eclipse.rcp.configuration')
	 * p2.addFeature('org.eclipse.platform')
	 * ```
	 */
	public void base(Object base) {
		this.base = base;
	}

	private List<Object> pluginPath = new ArrayList<>();

	/** Adds a directory from which to read. */
	public void addPluginPath(Object pluginPath) {
		this.pluginPath.add(pluginPath);
	}

	private JdkConfig config = new JdkConfig(getProject());

	/** Returns the JDK config for users to edit. */
	public JdkConfig getJdkConfig() {
		return config;
	}

	private Map<String, String> buildProperties = Maps.newLinkedHashMap();

	/** Extra properties to set in the build. */
	public Map<String, String> getProps() {
		return buildProperties;
	}

	List<SwtPlatform> platforms = SwtPlatform.getAll();

	/** Sets the platforms which we will build for. */
	public void setConfigs(SwtPlatform... platforms) {
		this.platforms = Arrays.asList(platforms);
	}

	private Action<PdeProductBuildConfig> productConfig;

	/** Copies the product and imgs from the given directory to the given path within the build directory. */
	public void product(Action<PdeProductBuildConfig> productConfig) {
		this.productConfig = productConfig;
	}

	@TaskAction
	public void build() throws Exception {
		Objects.requireNonNull(destination, "destination must not be null!");
		Objects.requireNonNull(base, "base must not be null!");

		// delete the buildDir and make a fresh directory
		File destination = getProject().file(this.destination);
		FileMisc.cleanDir(destination);

		File base = getProject().file(this.base);

		// setup build.properties
		PdeBuildProperties properties = new PdeBuildProperties();
		properties.setBasePlatform(SwtPlatform.getRunning());
		properties.setBuildDirectory(destination);
		properties.setProp("base", base.getAbsolutePath());
		properties.setConfigs(platforms); // for all configs
		properties.setJDK(config);

		List<File> pluginPaths = FileMisc.parseListFile(getProject(), pluginPath);
		properties.setPluginPaths(pluginPaths);

		// now that we've set the base values, give the product part a wack at it (if there is one)
		if (productConfig != null) {
			PdeProductBuildConfig product = new PdeProductBuildConfig(getProject());
			productConfig.execute(product);
			List<File> roots = new ArrayList<>();
			roots.add(base);
			roots.addAll(pluginPaths);
			product.setup(destination, properties, platforms, roots);
		}

		// set all the properties we'd like to set
		for (Map.Entry<String, String> entry : buildProperties.entrySet()) {
			properties.setProp(entry.getKey(), entry.getValue());
		}
		// write build.properties to the appropriate directory
		File buildDirProperties = new File(destination, "build.properties");
		Files.write(properties.getContent(), buildDirProperties, StandardCharsets.UTF_8);

		// generate and execute the PDE build command
		PdeInstallation installation = PdeInstallation.fromProject(getProject());
		installation.productBuildCmd(destination).runUsing(installation);
	}
}
