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
package com.diffplug.gradle.eclipse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ProjectDependency;

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
 * Can also be configured to do some other tricks involving replacing
 * binary dependencies with eclipse project dependencies:
 *
 * ```groovy
 * eclipseProjectDeps {
 *     replaceJar = true
 *     // when this is true, if a binary jar dependency is found with the
 *     // same name as a project dependency, then the binary jar dependency
 *     // will be removed, leaving only the project dependency
 *
 *     onlyIfHasJar = true
 *     // when this is true, a project dependency will be added only
 *     // if there is also a binary jar dependency with the same name
 *     //
 *     // this is helpful in a multiproject build, where you'd like
 *     // all projects which depend on `extlib.jar` to instead depend
 *     // on the `extlib` eclipse project.  You can add `extlib` as a
 *     // reference project for all of them, and it will only actually
 *     // be added to projects for which there is also an existing
 *     // binary dependency
 * }
 * ```
 */
public class ProjectDepsPlugin extends ProjectPlugin {
	@SuppressWarnings("unchecked")
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
				// the name (minus ".jar") of any jars on the classpath
				List<String> jarDeps = new ArrayList<>();

				// find the jars, and remove all existing referenced projects
				Node classpathNode = xmlProvider.asNode();
				Iterator<Node> classpathEntries = classpathNode.children().iterator();
				while (classpathEntries.hasNext()) {
					Node entry = classpathEntries.next();
					String path = (String) entry.attributes().get("path");
					if (path != null) {
						if (path.endsWith(".jar")) {
							// keep track of binary jars
							jarDeps.add(parseLibraryName(path));
						} else if (eclipseModel.getProject().getReferencedProjects().contains(path.substring(1))) {
							// remove all existing referenced projects
							classpathEntries.remove();
						}
					}
				}

				// map from referenced project to jar filename (for those which have been mapped)
				Map<String, String> eclipseToJar = new HashMap<>();
				if (extension.replaceJar || extension.onlyIfHasJar) {
					for (String projectDep : eclipseModel.getProject().getReferencedProjects()) {
						// find the longest jar which matches the project
						String matching = "";
						for (String jarDep : jarDeps) {
							if (jarDep.length() > matching.length() && jarDep.startsWith(projectDep)) {
								matching = jarDep;
							}
						}
						if (!matching.isEmpty()) {
							eclipseToJar.put(projectDep, matching);
						}
					}
				}

				// add the referenced projects with all of the required attributes
				for (String projectDep : eclipseModel.getProject().getReferencedProjects()) {
					// if onlyIfHasJar is true, and it doesn't contain the jar, bail
					if (extension.onlyIfHasJar && !eclipseToJar.containsKey(projectDep)) {
						continue;
					}
					// if replaceJar is true, and there's a jar to be removed, then remove it
					if (extension.replaceJar && eclipseToJar.containsKey(projectDep)) {
						String jar = "/" + eclipseToJar.get(projectDep) + ".jar";
						classpathEntries = classpathNode.children().iterator();
						while (classpathEntries.hasNext()) {
							Node entry = classpathEntries.next();
							String path = (String) entry.attributes().get("path");
							if (path != null && path.endsWith(jar)) {
								classpathEntries.remove();
							}
						}
					}
					// add the node
					Node entry = classpathNode.appendNode("classpathentry");
					entry.attributes().put("combineaccessrules", "true");
					entry.attributes().put("exported", "true");
					entry.attributes().put("kind", "src");
					entry.attributes().put("path", "/" + projectDep);
				}
			});
		});
	}

	static String parseLibraryName(String input) {
		Preconditions.checkArgument(input.endsWith(".jar"));
		int lastIdx = input.lastIndexOf('/');
		return input.substring(lastIdx + 1, input.length() - 4);
	}
}
