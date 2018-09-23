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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

import com.diffplug.common.base.*;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.bundling.Jar;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;

import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.ProjectPlugin;
import com.diffplug.gradle.ZipMisc;

/**
 * Generates a manifest using purely bnd, and outputs it for IDE consumption.
 *
 * Generating manifests by hand is a recipe for mistakes. Bnd does a fantastic
 * job generating all this stuff for you, but there's a lot of wiring required
 * to tie bnd into both Eclipse PDE and Gradle. Which is what Goomph is for!
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.osgi.bndmanifest'
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
        ProjectPlugin.getPlugin(proj, JavaPlugin.class);
        BndManifestExtension extension = proj.getExtensions().create(BndManifestExtension.NAME, BndManifestExtension.class);

        proj.afterEvaluate(project -> {

			// copyFromTask must be configured if copyTo is used
			Preconditions.checkArgument(extension.copyTo == null || extension.copyFromTask != null,
					"copyFromTask can not be null if copyTo is set. Please provide a source task.");

			final Jar copyFromTask = (extension.copyFromTask == null) ? null : getAsJar(proj, extension.copyFromTask);

			extension.includeTasks.forEach(name -> {

				Jar jarTask = getAsJar(proj, (String) name);

				// at the end of the jar, modify the manifest
				jarTask.doLast(unused -> {
					Errors.rethrow().run(() -> {
						byte[] manifest = getManifestContent(jarTask, extension).getBytes(StandardCharsets.UTF_8);
						// modify the jar
						Map<String, Function<byte[], byte[]>> toModify = ImmutableMap.of("META-INF/MANIFEST.MF", in -> manifest);
						ZipMisc.modify(jarTask.getArchivePath(), toModify, Predicates.alwaysFalse());
						// write manifest to the output resources directory
						Throwing.Consumer<Path> writeManifest = path -> {
							if (Files.exists(path)) {
								if (Arrays.equals(Files.readAllBytes(path), manifest)) {
									return;
								}
							}
							Files.createDirectories(path.getParent());
							Files.write(path, manifest);
						};
						writeManifest.accept(outputManifest(jarTask));

						// and maybe write it to `osgiBndManifest { copyTo }`.
						if (extension.copyTo != null && jarTask.equals(copyFromTask)) {
							writeManifest.accept(jarTask.getProject().file(extension.copyTo).toPath());
						}
					});
				});
			});

            // find the file that the user would like us to copy to (if any)
            if (extension.copyTo != null && copyFromTask != null) {
                copyFromTask.getOutputs().file(extension.copyTo);
            }
        });
    }

	private static Path outputManifest(Jar jarTask) {
		JavaPluginConvention javaConvention = jarTask.getProject().getConvention().getPlugin(JavaPluginConvention.class);
		SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		return main.getOutput().getResourcesDir().toPath().resolve("META-INF/MANIFEST.MF");
	}

	private static String getManifestContent(Jar jarTask, BndManifestExtension extension) throws Throwable {
		// if we don't want to merge, then delete the existing manifest so that bnd doesn't merge with it
		if (!extension.mergeWithExisting) {
			Files.deleteIfExists(outputManifest(jarTask));
		}
		// take the bnd action
		return BndManifestPlugin.takeBndAction(jarTask.getProject(), jarTask, jar -> {
			return StringPrinter.buildString(printer -> {
				try (OutputStream output = printer.toOutputStream(StandardCharsets.UTF_8)) {
					aQute.bnd.osgi.Jar.writeManifest(jar.getManifest(), printer.toOutputStream(StandardCharsets.UTF_8));
				} catch (Exception e) {
					throw Errors.asRuntime(e);
				}
			});
		});
	}

	/** Takes an action on a Bnd jar. */
	private static String takeBndAction(Project project, Jar jarTask, Throwing.Function<aQute.bnd.osgi.Jar, String> onBuilder) throws Exception, Throwable {
		try (Builder builder = new Builder()) {
			// set the base folder
			builder.setBase(project.getProjectDir());
			// copy all properties from jar.manifest.attributes into the bnd Builder
			Attributes attr = jarTask.getManifest().getEffectiveManifest().getAttributes();
			for (Map.Entry<String, Object> entry : attr.entrySet()) {
				builder.set(entry.getKey(), entry.getValue().toString());
			}

			// set the classpath for manifest calculation
			Set<File> runtimeConfig = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).getFiles();
			builder.addClasspath(runtimeConfig);

			// put the class files and resources into the jar
			JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
			SourceSetOutput main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME).getOutput();
			// delete empty folders so that bnd doesn't make Export-Package entries for them
			Set<String> includeresource = new HashSet<>();
			deleteEmptyFoldersIfExists(main.getResourcesDir());
			includeresource.add(fix(main.getResourcesDir()));
			for (File file : main.getClassesDirs()) {
				deleteEmptyFoldersIfExists(file);
				includeresource.add(fix(file));
			}
			builder.set(Constants.INCLUDERESOURCE, String.join(",", includeresource));

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
			return onBuilder.apply(builder.build());
		}
	}

	private static String fix(File file) {
		return file.getAbsolutePath().replace('\\', '/');
	}

	private static void deleteEmptyFoldersIfExists(File root) throws IOException {
		if (root.exists()) {
			FileMisc.deleteEmptyFolders(root);
		}
	}

	private static String dateQualifier() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddkkmm");
		return dateFormat.format(new Date());
	}

	private static Jar getAsJar(Project project, String taskName) {
		Task task = project.getTasks().getByName(taskName);
		Preconditions.checkArgument(task instanceof Jar,  "Task " + taskName + " must be a Jar derived task for generating BndManifest");
		return (Jar)task;
	}
}
