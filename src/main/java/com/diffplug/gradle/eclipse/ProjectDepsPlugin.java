/*
 * Copyright 2019 DiffPlug
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

import groovy.util.Node;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Iterables;
import com.diffplug.gradle.ProjectPlugin;

/**
 * Fixes an intermittent problem when generating eclipse
 * project files where dependencies on other projects within
 * the workspace aren't always resolved correctly within Eclipse.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.eclipse.projectdeps'
 * ```
 *
 * Can also be configured to replace binary dependencies with
 * eclipse project dependencies:
 *
 * ```groovy
 * eclipseProjectDeps {
 *     replaceWithProject('some-external-lib')
 *     replaceWithProject(['libA', 'libB', 'libC'])
 * }
 * ```
 */
public class ProjectDepsPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		ProjectDepsExtension extension = project.getExtensions().create(ProjectDepsExtension.NAME, ProjectDepsExtension.class);
		EclipseProjectPlugin.modifyEclipseProject(project, eclipseModel -> {
			// find the project's referenced projects and reference them explicitly in the eclipse model
			Task prepareEclipse = project.task("prepareEclipse");
			prepareEclipse.doLast(task -> {
				Set<String> referencedProjects = eclipseModel.getProject().getReferencedProjects();
				project.getConfigurations().stream()
						.flatMap(config -> config.getDependencies().stream())
						.filter(dep -> dep instanceof ProjectDependency)
						.forEach(dep -> {
							referencedProjects.add(dep.getName());
						});
			});
			// it's needed for generating the eclipseClasspath and eclipseProject
			Iterables.getOnlyElement(project.getTasksByName("eclipseClasspath", false)).dependsOn(prepareEclipse);
			Iterables.getOnlyElement(project.getTasksByName("eclipseProject", false)).dependsOn(prepareEclipse);

			// create robust classpath entries for all referenced projects
			eclipseModel.getClasspath().getFile().getXmlTransformer().addAction(xmlProvider -> {
				modifyClasspath(xmlProvider.asNode(), eclipseModel, extension);
			});
		});
	}

	@SuppressWarnings("unchecked")
	private void modifyClasspath(Node classpathNode, EclipseModel eclipseModel, ProjectDepsExtension extension) {
		// the name (minus ".jar") of any jars on the classpath
		List<String> jarDeps = new ArrayList<>();

		// find the jars, and remove all existing referenced projects
		Iterator<Node> classpathEntries = classpathNode.children().iterator();
		while (classpathEntries.hasNext()) {
			Node entry = classpathEntries.next();
			String path = (String) entry.attributes().get("path");
			if (path != null && !path.isEmpty()) {
				if (path.endsWith(".jar")) {
					// keep track of binary jars
					jarDeps.add(parseLibraryName(path));
				} else if (eclipseModel.getProject().getReferencedProjects().contains(path.substring(1))) {
					// remove all existing referenced projects
					classpathEntries.remove();
				}
			}
		}

		// define a function which adds a project properly
		Consumer<String> addProject = projectDep -> {
			Node entry = classpathNode.appendNode("classpathentry");
			entry.attributes().put("combineaccessrules", "true");
			entry.attributes().put("exported", "true");
			entry.attributes().put("kind", "src");
			entry.attributes().put("path", "/" + projectDep);
		};

		// add all explicitly referenced projects
		eclipseModel.getProject().getReferencedProjects().forEach(addProject);

		// map from referenced project to jar filename (for those which have been mapped)
		List<String> toReplace = new ArrayList<>(extension.jarsToReplace);
		// sort from longest to shortest
		Collections.sort(toReplace, Comparator.comparing(String::length).reversed().thenComparing(Function.identity()));
		for (String jarToReplace : toReplace) {
			// find the longest jar which matches the project
			Optional<String> matching = jarDeps.stream()
					.filter(dep -> dep.startsWith(jarToReplace))
					.max(Comparator.comparing(String::length));
			// if we found one
			if (matching.isPresent()) {
				// remove the jar from the classpath
				jarDeps.remove(matching.get());
				String jar = "/" + matching.get() + ".jar";
				classpathEntries = classpathNode.children().iterator();
				while (classpathEntries.hasNext()) {
					Node entry = classpathEntries.next();
					String path = (String) entry.attributes().get("path");
					if (path != null && path.endsWith(jar)) {
						classpathEntries.remove();
					}
				}
				// and add a project
				addProject.accept(jarToReplace);
			}
		}
	}

	static String parseLibraryName(String input) {
		Preconditions.checkArgument(input.endsWith(".jar"));
		int lastIdx = input.lastIndexOf('/');
		return input.substring(lastIdx + 1, input.length() - 4);
	}
}
