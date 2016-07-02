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
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ProjectPlugin;
import com.diffplug.gradle.pde.EclipseRelease;

/**
 * Downloads and sets up an Eclipse IDE.  Each IDE created by
 * Goomph stores its plugins centrally in {@link GoomphCacheLocations#bundlePool()}.
 * This means it only takes ~1MB of extra diskspace per IDE, so that you can install
 * many IDE's, each with their own settings and plugins, while being very efficient
 * with your disk and network resources.
 *
 * `gradlew ide` will launch the IDE.
 *
 * To create an IDE for java projects:
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.oomph.ide'
 * oomphIde {
 *     jdt {}
 * }
 * ```
 *
 * For an Eclipse Plugin project with a target platform:
 * 
 * ```groovy
 * oomphIde {
 *     pde {
 *         targetplatform {
 *             installation('target.maven/build')
 *             installation('target.p2/build/p2asmaven/p2')
 *         }
 *     }
 * }
 * ```
 * 
 * You can also set the icon and splashscreen used to launch
 * the IDE for your project, as well as detailed settings.
 * 
 * ```groovy
 * oomphIde {
 *     icon   'images/icon.png'
 *     splash 'images/mascot.png'
 *     jdt {}
 *     eclipseIni {
 *         vmargs('-Xmx2g')	// IDE can have up to 2 gigs of RAM
 *     }
 *     workspaceProp '.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs', { 
 *         it.put('themeid', 'org.eclipse.e4.ui.css.theme.e4_classic')
 *     }
 * }
 * ```
 * 
 * The Eclipse IDE has a broad ecosystem.  You can use Goomph
 * to configure any combination of pieces from this ecosystem, but
 * this requires detailed knowledge of the update sites and installable
 * units that these projects use.
 * 
 * ```groovy
 * oomphIde {
 *     p2.addRepo('http://download.eclipse.org/buildship/updates/e45/releases/1.0')
 *     p2.addIU('org.eclipse.buildship')
 * }
 * ```
 * 
 * ### Which version of eclipse will it use?
 * 
 * If you specify a repository manually, it will use that version.
 * 
 * ```groovy
 * oomphIde {
 *     // Use Mars SR2
 *     p2.addRepoOfficial('4.5.2')
 *     // Use the latest Neon milestone
 *     p2.addRepo('http://download.eclipse.org/eclipse/updates/4.6milestones')
 *     jdt {}
 * }
 * ```
 * 
 * If you don't specify any repositories, then the latest and greatest
 * official eclipse release will automatically be used, currently {@link EclipseRelease#LATEST}.
 * 
 * ### Which projects get imported?
 * 
 * Any eclipse projects which are defined in this project will be automatically
 * imported.  When creating the ide, these are the task dependencies:
 * 
 * `ide` -> `ideSetup` -> `eclipse`
 * 
 * If you have a multiproject build, you can do the following:
 * 
 * ```groovy
 * oomphIde {
 *     // adds the eclipse project from the given project
 *     addProject(':gradle-project:path')
 *     // adds eclipse projects from every Gradle project in the build
 *     addAllProjects()
 * }
 * ```
 * 
 * ### How do I control the details?
 * 
 * See {@link OomphIdeExtension} for the full DSL.
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
			extension.addDependency(p);
			// tie ide to idesetup iff setup is required
			if (!extension.isClean()) {
				ide.dependsOn(ideSetup);
			}
		});
	}

	static final String IDE_SETUP = "ideSetup";
	static final String IDE = "ide";
}
