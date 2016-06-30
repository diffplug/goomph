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
 * Downloads and sets up an Eclipse IDE.
 * 
 * `gradlew ide` will launch the IDE.
 * 
 * Note that it will be in the "Resources Perspective" the first time you open it,
 * and you'll probably want to switch to the Java perspective, look in the top
 * right of the window for this:
 *
 * ![Perspective bar](http://help.eclipse.org/mars/topic/org.eclipse.platform.doc.user/images/Image211_perspective.png)
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.oomph.ide'
 * oomphIde {
 *     // setup the components to download
 *     p2.addRepoEclipse('4.6.0')
 *     p2.addIU('org.eclipse.platform.ide')
 *     p2.addFeature('org.eclipse.jdt')
 *     p2.addFeature('org.eclipse.pde')
 *
 *     // add buildship
 *     p2.addRepo('http://download.eclipse.org/buildship/updates/e45/releases/1.0')
 *     p2.addFeature('org.eclipse.buildship', '1.0.16.v20160615-0737')
 *
 *     eclipseIni {
 *         vmargs('-Xmx2g')	// IDE can have 2 gigs of RAM, if it wants
 *     }
 *
 *     // determine which projects to import.  There are two options:
 *     // 1) It will automatically find eclipse tasks in this same project
 *     // 2) If you call "addAllProjects()" then it will add all eclipse tasks in all projects
 *     addAllProjects()
 *
 *     // if you're using PDE, then you'll need a targetplatform
 *     targetplatform 'goomph-target', {
 *         installation('target.frommaven/build')
 *         installation('target.fromp2/build/p2asmaven/p2')
 *     }
 *
 *     classicTheme()   // oldschool cool
 *     niceText()       // nice fonts and visible whitespace
 * }
 * ```
 */
public class OomphIdePlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		OomphIdeExtension extension = project.getExtensions().create(OomphIdeExtension.NAME, OomphIdeExtension.class, project);
		// ideSetup
		Task ideSetup = project.getTasks().create(IDE_SETUP);
		ideSetup.doFirst(unused -> {
			Errors.rethrow().run(extension::ideSetup);
		});
		// ide
		Task ide = project.getTasks().create(IDE);
		ide.doFirst(unused -> {
			Errors.rethrow().run(extension::ide);
		});
		project.afterEvaluate(p -> {
			// ideSetup -> eclipse
			project.getTasks().all(task -> {
				if ("eclipse".equals(task.getName())) {
					ideSetup.dependsOn(task);
				}
				if (task instanceof GenerateEclipseProject) {
					extension.addProjectFile(((GenerateEclipseProject) task).getOutputFile());
				}
			});
			// tie ide to idesetup iff setup is required
			if (!extension.isClean()) {
				ide.dependsOn(ideSetup);
			}
		});
	}

	static final String IDE_SETUP = "ideSetup";
	static final String IDE = "ide";
}
