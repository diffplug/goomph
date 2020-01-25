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


import com.diffplug.gradle.LegacyPlugin;
import com.diffplug.gradle.ProjectPlugin;
import org.gradle.api.Project;

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
 * apply plugin: 'com.diffplug.eclipse.excludebuildfolder'
 * ```
 * 
 * If you'd like to exclude more than just the build folder,
 * you might want to look at the more general {@link ResourceFiltersPlugin}.
 */
public class ExcludeBuildFolderPlugin extends ProjectPlugin {
	public static class Legacy extends LegacyPlugin {
		public Legacy() {
			super(ExcludeBuildFolderPlugin.class, "com.diffplug.eclipse.excludebuildfolder");
		}
	}

	@Override
	protected void applyOnce(Project project) {
		LegacyPlugin.applyForCompat(project, Legacy.class);
		ResourceFiltersPlugin resourceFilters = project.getPlugins().apply(ResourceFiltersPlugin.class);
		resourceFilters.extension.filters.add(ResourceFilter.exclude().folders().name("build"));
	}
}
