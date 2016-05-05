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
package com.diffplug.gradle.swt;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import com.diffplug.gradle.ProjectPlugin;

/**
 * Adds the platform-specific SWT jars which are appropriate for the
 * native system to the compile classpath.
 * <p>
 * Currently supports only 4.4.2 and 4.5.2.
 */
public class NativeDepsPlugin extends ProjectPlugin {
	@Override
	public void applyOnce(Project project) {
		ProjectPlugin.getPlugin(project, JavaPlugin.class);

		// create the NativeDepsExtension
		NativeDepsExtension extension = project.getExtensions().create(NativeDepsExtension.NAME, NativeDepsExtension.class);
		project.afterEvaluate(proj -> {
			// add the update site as an ivy repository
			proj.getRepositories().ivy(ivyConfig -> {
				ivyConfig.artifactPattern(extension.updateSite() + "plugins/[artifact]_[revision].[ext]");
			});

			// add all of SWT's dependencies 
			for (String dep : NativeDepsExtension.DEPS) {
				proj.getDependencies().add("compile", extension.fullDep(dep));
			}
		});
	}
}
