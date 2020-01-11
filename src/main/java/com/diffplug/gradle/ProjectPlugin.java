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
package com.diffplug.gradle;


import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.tree.TreeDef;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** Base implementation of a Plugin which prevents double-application. */
public abstract class ProjectPlugin implements Plugin<Project> {
	@Override
	public final void apply(Project project) {
		// ensure we don't double-apply the plugin
		if (project.getPlugins().hasPlugin(this.getClass())) {
			return;
		}
		project.afterEvaluate(GoomphCacheLocations::initFromProject);
		// apply the plugin once
		applyOnce(project);
	}

	/** Plugin application, which is guaranteed to execute only once. */
	protected abstract void applyOnce(Project project);

	/** Returns the instance of the given plugin, by returning the existing or applying brand new, as appropriate. */
	public static <T extends Plugin<?>> T getPlugin(Project project, Class<T> pluginClazz) {
		// make sure the eclipse plugin has been applied
		if (project.getPlugins().hasPlugin(pluginClazz)) {
			return project.getPlugins().getPlugin(pluginClazz);
		} else {
			return project.getPlugins().apply(pluginClazz);
		}
	}

	/** A TreeDef for projects. */
	public static TreeDef.Parented<Project> treeDef() {
		return TreeDef.Parented.of(p -> ImmutableList.copyOf(p.getChildProjects().values()), Project::getParent);
	}
}
