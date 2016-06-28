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
package com.diffplug.gradle.oomph;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.plugins.ide.eclipse.GenerateEclipseProject;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.ProjectPlugin;

/**
 * Oomph IDE.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.oomph.ide'
 * oomphIde {
 *     // setup the components to download
 *     p2.addRepo(EclipseRelease.official('4.5.2').updateSite())
 *     p2.addIU('org.eclipse.platform.ide')
 *     p2.addFeature('org.eclipse.jdt')
 *     p2.addFeature('org.eclipse.pde')
 * 
 *     // determine which projects to import.  There are two options:
 *     // 1) It will automatically find eclipse tasks in this same project
 *     // 2) If you call "addAllProjects()" then it will add all eclipse tasks in all projects
 *     addAllProjects()
 *     targetplatform {
 *         installation('target.frommaven/build')
 *         installation('target.fromp2/build/p2asmaven/p2')
 *     }
 *     // sets up the classic look
 *     classicTheme()
 * }
 * ```
 */
public class OomphIdePlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		OomphIdeExtension extension = project.getExtensions().create(OomphIdeExtension.NAME, OomphIdeExtension.class, project);
		Task setupIde = project.getTasks().create(SETUP);
		setupIde.doFirst(unused -> {
			Errors.rethrow().run(extension::setup);
		});

		Task runIde = project.getTasks().create(RUN);
		runIde.dependsOn(setupIde);
		runIde.doFirst(unused -> {
			Errors.rethrow().run(extension::run);
		});

		// setupIde depends on the eclipse tasks in this project
		project.getTasks().all(task -> {
			if ("eclipse".equals(task.getName())) {
				setupIde.dependsOn(task);
			}
			if (task instanceof GenerateEclipseProject) {
				extension.addProjectFile(((GenerateEclipseProject) task).getOutputFile());
			}
		});
	}

	static final String SETUP = "ideSetup";
	static final String RUN = "ide";
}
