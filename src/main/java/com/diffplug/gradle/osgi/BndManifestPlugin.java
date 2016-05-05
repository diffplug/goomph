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
package com.diffplug.gradle.osgi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.ProjectPlugin;

/**
 * Uses Bnd to create a manifest.  jar.manifest.attributes is passed
 * to bnd directly as properties.  All classes and resources
 * will be included in the final jar.
 * <p>
 * Many thanks to JRuyi and Agemo Cui for their excellent
 * <a href="https://github.com/jruyi/osgibnd-gradle-plugin">osgibnd-gradle-plugin</a>.
 * This plugin follows the template set by their plugin quite deliberately.
 */
public class BndManifestPlugin extends ProjectPlugin {
	private static final String PATH_MANIFEST = "META-INF/MANIFEST.MF";

	@Override
	protected void applyOnce(Project proj) {
		File manifestFile = proj.file(PATH_MANIFEST);
		proj.afterEvaluate(project -> {
			ProjectPlugin.getPlugin(project, JavaPlugin.class);
			Jar jarTask = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
			jarTask.deleteAllActions();
			jarTask.getInputs().properties(jarTask.getManifest().getEffectiveManifest().getAttributes());
			jarTask.getOutputs().file(jarTask.getArchivePath());
			jarTask.doLast(unused -> {
				takeBndAction(project, jar -> {
					createParents(jarTask.getArchivePath());
					jar.write(jarTask.getArchivePath());
					writeFile(manifestFile, jar::writeManifest);
				});
			});
		});
	}

	/** Takes an action on a Bnd jar. */
	private static void takeBndAction(Project project, Throwing.Consumer<aQute.bnd.osgi.Jar> onBuilder) {
		ProjectPlugin.getPlugin(project, JavaPlugin.class);
		Jar jarTask = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
		Errors.rethrow().run(() -> {
			try (Builder builder = new Builder()) {
				// set the base folder
				builder.setBase(project.getProjectDir());
				// copy all properties from jar.manifest.attributes into the bnd Builder
				Attributes attr = jarTask.getManifest().getEffectiveManifest().getAttributes();
				for (Map.Entry<String, Object> entry : attr.entrySet()) {
					builder.set(entry.getKey(), entry.getValue().toString());
				}

				// set the classpath for manifest calculation
				Set<File> runtimeConfig = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME).getFiles();
				builder.addClasspath(runtimeConfig);

				// put the class files and resources into the jar
				JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
				SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
				builder.set(Constants.INCLUDERESOURCE, main.getOutput().getClassesDir() + "," + main.getOutput().getResourcesDir());

				// set the version
				if (builder.getBundleVersion() == null) {
					try {
						builder.setBundleVersion(project.getVersion().toString());
					} catch (Exception e) {
						project.getLogger().warn(e.getMessage());
						builder.setBundleVersion("0.0.0.ERRORSETVERSION");
					}
				}

				// take an action with the builder
				onBuilder.accept(builder.build());
			}
		});
	}

	/** Writes to the given file. */
	private static void writeFile(File file, Throwing.Consumer<OutputStream> writer) throws Throwable {
		createParents(file);
		try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
			writer.accept(output);
		}
	}

	/** Creates all parent files for the given file. */
	private static void createParents(File file) {
		file.getParentFile().mkdirs();
	}
}
