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

import java.util.Iterator;

import org.gradle.api.Project;
import org.gradle.api.XmlProvider;

import groovy.util.Node;

import com.diffplug.gradle.ProjectPlugin;

/**
 * Starting with gradle version [4.4, gradle creates separate output](https://docs.gradle.org/4.4/release-notes.html#eclipse-plugin-separates-output-folders)
 * folders for each source folder.
 * 
 * There are benefits to this, but it also seems to have some downsides:
 * 
 * - the compiled artifacts in `bin/` show up in the "Open Resource" dialog, and are hard to suppress
 * - intermittent failures about `The project was not built due to "Resource already exists on disk: '/projName/bin/default/...`
 * 
 * These seem to be eclipse bugs rather than gradle bugs, but if you don't really need the separate output folders,
 * then it would be nice to revert your eclipse projects back to the old style.  That's what this does:
 * 
 * ```gradle
 * // reverts eclipse source folder entries in .classpath to match gradle <= 4.3 
 * apply plugin: 'com.diffplug.gradle.eclipse.classic'
 * ```
 */
public class GradleClassicPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		EclipseProjectPlugin.modifyEclipseProject(project, eclipseModel -> {
			eclipseModel.getClasspath().getFile().getXmlTransformer().addAction(this::classic);
		});
	}

	@SuppressWarnings("unchecked")
	private void classic(XmlProvider xmlProvider) {
		Node classpathNode = xmlProvider.asNode();
		Iterator<Node> classpathEntries = classpathNode.children().iterator();
		while (classpathEntries.hasNext()) {
			Node entry = classpathEntries.next();
			String kind = (String) entry.attribute("kind");
			if ("output".equals(kind)) {
				entry.attributes().put("path", "bin");
			} else if ("src".equals(kind)) {
				entry.attributes().remove("output");
				entry.children().clear();
			}
		}
	}
}
