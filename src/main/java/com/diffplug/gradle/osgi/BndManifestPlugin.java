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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
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
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.ProjectPlugin;

/**
 * Generates a manifest using purely bnd, and outputs it for IDE consumption.
 * 
 * Generating manifests by hand is a recipe for mistakes. Bnd does a fantastic
 * job generating all this stuff for you, but there's a lot of wiring required
 * to tie bnd into both Eclipse PDE and Gradle. Which is what Goomph is for!
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.eclipse.bndmanifest'
 * // Pass headers and bnd directives: http://www.aqute.biz/Bnd/Format
 * jar.manifest.attributes(
 *     '-exportcontents': 'com.diffplug.*',
 *     '-removeheaders': 'Bnd-LastModified,Bundle-Name,Created-By,Tool,Private-Package',
 *     'Import-Package': '!javax.annotation.*,*',
 *     'Bundle-SymbolicName': "${project.name};singleton:=true",
 * )
 * // The block below is optional.
 * osgiBndManifest {
 *     // The manifest will always be included in the built jar
 *     // at the proper 'META-INF/MANIFEST.MF' location.  But if
 *     // you'd like to easily see the manifest for debugging or
 *     // to help an IDE, you can ask gradle to copy the manifest
 *     // into your source tree.
 *     copyTo 'src/main/resources/META-INF/MANIFEST.MF'

 *     // By default, the existing manifest is completely ignored.
 *     // The line below will cause the existing manifest's fields
 *     // to be merged with the fields set by bnd.
 *     mergeWithExisting true  
 * }
 * ```
 * 
 * Besides passing raw headers and bnd directives, this plugin also takes the following actions:
 * 
 * * Passes the project version to bnd if {@code Bundle-Version} hasn't been set explicitly.
 * * Replaces `-SNAPSHOT` in the version with `.IyyyyMMddkkmm` (to-the-minute timestamp).
 * * Passes the {@code runtime} configuration's classpath to bnd for manifest calculation.
 * * Instructs bnd to respect the result of the {@code processResources} task.
 * 
 * Many thanks to JRuyi and Agemo Cui for their excellent
 * [osgibnd-gradle-plugin](https://github.com/jruyi/osgibnd-gradle-plugin).
 * This plugin follows the template set by their plugin, but with fewer automagic
 * features and tighter integrations with IDEs and gradle's resources pipeline.
 */
public class BndManifestPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project proj) {
		BndManifestExtension extension = proj.getExtensions().create(BndManifestExtension.NAME, BndManifestExtension.class);
		proj.afterEvaluate(project -> {
			// find the file that the user would like us to copy to (if any)
			Optional<File> copyTo = Optional.ofNullable(extension.copyTo).map(proj::file);

			ProjectPlugin.getPlugin(project, JavaPlugin.class);
			Jar jarTask = (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
			jarTask.deleteAllActions();
			jarTask.getInputs().properties(jarTask.getManifest().getEffectiveManifest().getAttributes());
			jarTask.getOutputs().file(jarTask.getArchivePath());
			copyTo.ifPresent(jarTask.getOutputs()::file);
			jarTask.doLast(Errors.rethrow().wrap(unused -> {
				// find the location of the manifest in the output resources directory
				JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
				SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
				Path outputManifest = main.getOutput().getResourcesDir().toPath().resolve("META-INF/MANIFEST.MF");
				// if we don't want to merge, then delete the existing manifest so that bnd doesn't merge with it
				if (!extension.mergeWithExisting) {
					Files.deleteIfExists(outputManifest);
				}
				// take the bnd action 
				takeBndAction(project, jar -> {
					// write out the jar file
					createParents(jarTask.getArchivePath());
					jar.write(jarTask.getArchivePath());
					// write out the manifest to the resources output directory for the test task (and others)
					writeFile(outputManifest.toFile(), jar::writeManifest);
					// write the manifest to copyTo, if we're supposed to
					if (copyTo.isPresent()) {
						writeFile(copyTo.get(), jar::writeManifest);
					}
				});
			})::accept);
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
		FileMisc.mkdirs(file.getParentFile());
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
						String version = project.getVersion().toString();
						if (version.endsWith("-SNAPSHOT")) {
							version = version.replace("-SNAPSHOT", ".I" + dateQualifier());
						}
						builder.setBundleVersion(version);
					} catch (Exception e) {
						project.getLogger().warn(e.getMessage() + "  Must be 'major.minor.micro.qualifier'");
						builder.setBundleVersion("0.0.0.ERRORSETVERSION");
					}
				}
				// take an action with the builder
				onBuilder.accept(builder.build());
			}
		});
	}

	static String dateQualifier() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmm");
		return dateFormat.format(new Date());
	}
}
