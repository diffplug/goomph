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
package com.diffplug.gradle.eclipse;


import com.diffplug.gradle.LegacyPlugin;
import com.diffplug.gradle.ProjectPlugin;
import groovy.util.Node;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.gradle.api.Project;

/**
 * Eclipse projects can have [resource filters](https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Fconcepts%2Fresourcefilters.htm)
 * which include or exclude certain files.
 * 
 * ![Screenshot](https://i.stack.imgur.com/7eIVE.png)
 * 
 * This plugin allows you to easily configure these settings.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.eclipse.resourcefilters'
 * eclipseResourceFilters {
 *     exclude().folders().name('build')
 *     include().folders().projectRelativePath('main/src/*')
 *     exclude().files().name('*.class').recursive()
 * }
 * ```
 * 
 * For full details on what filters you can create, see {@link ResourceFilter}.
 */
public class ResourceFiltersPlugin extends ProjectPlugin {
	public static class Legacy extends LegacyPlugin {
		public Legacy() {
			super(ResourceFiltersPlugin.class, "com.diffplug.eclipse.resourcefilters");
		}
	}

	ResourceFiltersExtension extension;

	@SuppressWarnings("unchecked")
	@Override
	protected void applyOnce(Project project) {
		LegacyPlugin.applyForCompat(project, Legacy.class);
		extension = project.getExtensions().create(ResourceFiltersExtension.NAME, ResourceFiltersExtension.class);
		EclipseProjectPlugin.modifyEclipseProject(project, eclipseModel -> {
			eclipseModel.getProject().getFile().getXmlTransformer().addAction(xmlProvider -> {
				Node rootNode = (Node) xmlProvider.asNode();
				// remove the old filteredResources
				List<Node> toRemove = ((List<Node>) rootNode.children()).stream()
						.filter(Objects::nonNull)
						.filter(node -> FILTERED_RESOURCES.equals(node.name()))
						.collect(Collectors.toList());
				toRemove.forEach(rootNode::remove);
				// now add ours
				Node filteredResources = rootNode.appendNode(FILTERED_RESOURCES);
				for (ResourceFilter toExclude : extension.filters) {
					toExclude.appendToFilteredResources(filteredResources);
				}
			});
		});
	}

	static final String FILTERED_RESOURCES = "filteredResources";
}
