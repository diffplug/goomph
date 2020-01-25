/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.eclipse;


import com.diffplug.common.base.Suppliers;
import com.diffplug.gradle.GroovyCompat;
import com.diffplug.gradle.LegacyPlugin;
import com.diffplug.gradle.ProjectPlugin;
import groovy.util.Node;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.UnknownPluginException;
import org.gradle.language.jvm.tasks.ProcessResources;

/**
 * Uses [`build.properties`](https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm)
 * to control a gradle build, and fixes the eclipse project classpath to include binary assets specified in `build.properties`.
 * 
 * Eclipse PDE uses a `build.properties` file to control the build process.  Even if you aren't using PDE for
 * your build, the IDE will throw warnings if you don't keep the `build.properties` up to date.
 * 
 * This plugin reads the `build.properties` file, and uses that to setup the Gradle `processResources` task.
 * It also ensures that these resources are available on the IDE's classpath.  This way your `build.properties`
 * can be the single source of truth for all the binary assets inside your plugin.
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.eclipse.buildproperties'
 * ```
 */
public class BuildPropertiesPlugin extends ProjectPlugin {
	public static class Legacy extends LegacyPlugin {
		public Legacy() {
			super(BuildPropertiesPlugin.class, "com.diffplug.eclipse.buildproperties");
		}
	}

	private Project project;

	@SuppressWarnings("unchecked")
	@Override
	protected void applyOnce(Project project) {
		LegacyPlugin.applyForCompat(project, Legacy.class);
		this.project = project;

		EclipseProjectPlugin.modifyEclipseProject(project, eclipseModel -> {
			// add <classpathentry including="META-INF/|OSGI-INF/" kind="src" path=""/>
			eclipseModel.getClasspath().getFile().getXmlTransformer().addAction(xmlProvider -> {
				Node entry = xmlProvider.asNode().appendNode("classpathentry");
				entry.attributes().put("kind", "src");
				entry.attributes().put("path", "");
				String including = getBinIncludes().stream().collect(Collectors.joining("|"));
				entry.attributes().put("including", including);
			});

			// update processResources based on build.properties
			project.getPlugins().apply(JavaPlugin.class);
			ProcessResources task = (ProcessResources) project.getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
			// handle the build.properties includes
			//AbstractCopyTask copyTask = task.from(project.getProjectDir());
			task.from(project.getProjectDir(), GroovyCompat.<CopySpec> closureFrom(task, spec -> {
				for (String binInclude : getBinIncludes()) {
					if (binInclude.endsWith("/")) {
						// include all of a specified folder
						spec = spec.include(binInclude + "**");
					} else {
						// or a single specified item
						spec = spec.include(binInclude);
					}
				}
				return spec;
			}));
			// handle the eclipse built-ins (properties files embedded in the src directory)
			task.from("src", GroovyCompat.<CopySpec> closureFrom(task, spec -> {
				return spec.include("**").exclude("**/*.java");
			}));
		});
	}

	/** Returns the bin.incldes for this project. */
	public static List<String> getBinIncludes(Project project) {
		BuildPropertiesPlugin plugin;
		try {
			plugin = project.getPlugins().getAt(BuildPropertiesPlugin.class);
		} catch (UnknownPluginException e) {
			plugin = project.getPlugins().apply(BuildPropertiesPlugin.class);
		}
		return plugin.getBinIncludes();
	}

	/** Returns the bin.includes from the build.properties file. */
	public List<String> getBinIncludes() {
		return binIncludes.get();
	}

	private final Supplier<List<String>> binIncludes = Suppliers.memoize(() -> {
		// parse build.properties and put it into binIncludes
		File buildProperties = project.file("build.properties");
		if (!buildProperties.exists()) {
			throw new IllegalArgumentException("There is no 'build.properties' file - do not apply 'com.diffplug.eclipse.buildproperties' to this project");
		}

		Properties parsedProperties = new Properties();
		try (InputStream stream = new BufferedInputStream(new FileInputStream(buildProperties))) {
			parsedProperties.load(stream);
		} catch (IOException e) {
			project.getLogger().warn(e.getMessage());
			return Collections.emptyList();
		}

		String raw = (String) parsedProperties.get("bin.includes");
		List<String> list = new ArrayList<>(Arrays.asList(raw.split(",")));
		// ignore the catch-all
		list.remove(".");
		return list;
	});
}
