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

import java.util.Iterator;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ProjectDependency;

import groovy.util.Node;

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
 */
public class ProjectDepsPlugin extends ProjectPlugin {
	@SuppressWarnings("unchecked")
	@Override
	protected void applyOnce(Project project) {
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
				Node classpathNode = xmlProvider.asNode();
				// remove any existing referenced projects
				Iterator<Node> classpathEntries = classpathNode.children().iterator();
				while (classpathEntries.hasNext()) {
					Node entry = classpathEntries.next();
					String path = (String) entry.attributes().get("path");
					if (path != null) {
						if (eclipseModel.getProject().getReferencedProjects().contains(path.substring(1))) {
							classpathEntries.remove();
						}
					}
				}
				// add the new referenced projects with all of the required attributes
				for (String projectDep : eclipseModel.getProject().getReferencedProjects()) {
					Node entry = classpathNode.appendNode("classpathentry");
					entry.attributes().put("combineaccessrules", "true");
					entry.attributes().put("exported", "true");
					entry.attributes().put("kind", "src");
					entry.attributes().put("path", "/" + projectDep);
				}
			});
		});
	}
}
