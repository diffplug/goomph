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

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.ProjectPlugin;

/**
 * Oomph IDE.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.oomph.ide'
 * oomphIde {
 *     p2.addRepo(EclipseRelease.official('4.5.2').updateSite())
 *     p2.addRepo('http://download.eclipse.org/buildship/
 *                 updates/e45/releases/1.0')
 *     p2.addIU('org.eclipse.sdk.ide')
 *     p2.addFeature('org.eclipse.buildship')
 *     addAllProjects()
 *     targetplatform {
 *         installation('target.frommaven/build')
 *         installation('target.fromp2/build/goomph-p2asmaven/p2')
 *     }
 * }
 * ```
 */
public class OomphIdePlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		OomphIdeExtension extension = project.getExtensions().create(OomphIdeExtension.NAME, OomphIdeExtension.class, project);

		Task setupIde = project.getTasks().create("ideSetup");
		setupIde.doFirst(unused -> {
			Errors.rethrow().run(extension::setup);
		});

		Task runIde = project.getTasks().create("ide");
		runIde.dependsOn(setupIde);
		runIde.doFirst(unused -> {
			Errors.rethrow().run(extension::run);
		});
	}
}
