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

import org.gradle.api.Project;

import groovy.util.Node;

import com.diffplug.gradle.ProjectPlugin;

/**
 * Creates eclipse project files which excludes the gradle build
 * folder from Eclipse's resource indexing.
 * 
 * If you hit `Ctrl + R` in eclipse, you'll get a fuzzy
 * search for resources in your workspace.  This will include
 * class files and other artifacts in the gradle build folder,
 * which is usually not desirable.  To fix:
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.eclipse.excludebuildfolder'
 * ```
 */
public class ExcludeBuildFolderPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		EclipseProjectPlugin.modifyEclipseProject(project, eclipseModel -> {
			// create a filterResources node
			eclipseModel.getProject().getFile().getXmlTransformer().addAction(xmlProvider -> {
				Node filterNode = xmlProvider.asNode().appendNode("filteredResources").appendNode("filter");
				filterNode.appendNode("id", project.getName().hashCode()); // any random string will work, we'll use the project name's hash
				filterNode.appendNode("name", "");
				filterNode.appendNode("type", 10);
				// make sure that it matches the build folder
				Node matcher = filterNode.appendNode("matcher");
				matcher.appendNode("id", "org.eclipse.ui.ide.multiFilter");
				matcher.appendNode("arguments", "1.0-name-matches-false-false-build");
			});
		});
	}
}
