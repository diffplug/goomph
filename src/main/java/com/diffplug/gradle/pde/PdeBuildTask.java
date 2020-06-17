/*
 * Copyright (C) 2016-2020 DiffPlug
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


import com.diffplug.common.collect.Maps;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.eclipserunner.EclipseApp;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.Actions;

/**
 * Runs PDE build to make an RCP application or a p2 repository.
 *
 * Your project must have defined `GOOMPH_PDE_VER`, see
 * {@link PdeInstallation#fromProject(org.gradle.api.Project)}
 * for details.
 *
 * ```groovy
 * import com.diffplug.gradle.pde.*
 * import com.diffplug.gradle.ZipMisc
 * 
 * task buildP2(type: PdeBuildTask) {
 *     // set the base platform
 *     base(rootProject.file('target.fromp2/build/p2asmaven/p2runnable'))
 *     // configure where the projects will come from
 *     addPluginPath(rootProject.file('target.frommaven/build'))
 *     // and where they will go
 *     destination(P2_DIR)
 *     // specify that this is a product build
 *     product {
 *         id 'com.diffplug.rcpdemo.product'
 *         version rootProject.version
 *         productPluginDir rootProject.file('com.diffplug.rcpdemo')
 *         productFileWithinPlugin 'rcpdemo.product'
 *         explicitVersionPolicy({
 *             it.resolve('com.google.guava', '17.0.0', '18.0.0').with('17.0.0', '18.0.0')
 *         })
 *     }
 *     // set the build properties to be appropriate for p2
 *     props['p2.build.repo'] = 'file://' + project.file(P2_DIR).absolutePath
 *     props['p2.gathering'] =    'true'
 *     props['skipDirector'] =    'true'
 *     props['runPackager'] = 'false'
 *     props['groupConfigurations'] = 'true'
 *
 *     app {
 *         it.consolelog()
 *         it.addArgs('-debug')
 *     }
 *     // p2.compress doesn't work, so we'll do it manually
 *     doLast {
 *         def compressXml = { name ->
 *             def xml = project.file(P2_REPO_DIR + "/${name}.xml")
 *             def jar = project.file(P2_REPO_DIR + "/${name}.jar")
 *             ZipMisc.zip(xml, "${name}.xml", jar)
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

	private Action<EclipseApp> appModifier = Actions.doNothing();

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
	@Internal
	public JdkConfig getJdkConfig() {
		return config;
	}

	private Map<String, String> buildProperties = Maps.newLinkedHashMap();

	/** Extra properties to set in the build. */
	@Internal
	public Map<String, String> getProps() {
		return buildProperties;
	}

	List<SwtPlatform> platforms = SwtPlatform.getAll();

	/** Sets the platforms which we will build for. */
	public void setConfigs(SwtPlatform... platforms) {
		this.platforms = Arrays.asList(platforms);
	}

	private Action<PdeProductBuildConfig> productConfig;

	/** Allows for fine-grained manipulation of the pde operation. */
	public void app(Action<EclipseApp> antModifier) {
		this.appModifier = Objects.requireNonNull(antModifier);
	}

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
		EclipseApp app = installation.productBuildCmd(destination);
		appModifier.execute(app);
		app.runUsing(installation);
	}
}
