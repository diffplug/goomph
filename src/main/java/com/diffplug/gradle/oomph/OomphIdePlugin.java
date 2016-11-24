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

/**
 * Downloads and sets up an Eclipse IDE.  Each IDE created by
 * Goomph stores its plugins centrally in {@link GoomphCacheLocations#bundlePool()}.
 * This means it only takes ~1MB of extra diskspace per IDE, so that you can install
 * many IDE's, each with their own settings and plugins, while being very efficient
 * with your disk and network resources.
 *
 * - `gradlew ide` launches the IDE, after running any required setup tasks.
 *
 * To create an IDE for java projects (see {@link ConventionJdt} for more JDT options).
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.oomph.ide'
 * oomphIde {
 *     repoEclipseLatest()
 *     jdt {}
 * }
 * ```
 *
 * For an Eclipse Plugin project (see {@link ConventionPde} for more JDT options).
 * 
 * ```groovy
 * oomphIde {
 *     repoEclipse '4.5.2'
 *     pde {}
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
 *     repoEclipseLatest()
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
 *     repo 'http://download.eclipse.org/buildship/updates/e45/releases/1.0'
 *     iu   'org.eclipse.buildship'
 * }
 * ```
 * 
 * ## What if I want to change my setup
 * 
 * It helps to know a little about the guts of the tasks.
 * 
 * `ide` -> `ideSetupWorkspace` -> `ideSetupP2`
 * 
 * - `ideSetupP2` installs plugins and updates their versions.
 *     + If you change something about the p2 model or the icons,
 *       this will rerun to generate exactly the plugins which
 *       have been specified.
 * - `ideSetupWorkspace` imports the projects and sets user settings.
 *     + If you change the projects or user settings, this will not rerun
 *       automatically, because that would wipe out any changes you've made
 *       manually while using the IDE.  If you want to wipe out the workspace,
 *       you can run `ideClean` and it will wipe the workspace.
 * - `ide` runs the IDE.
 * 
 * ## Which projects get imported?
 * 
 * If the gradle project to which you applied this plugin
 * also contains an eclipse project, it will automatically
 * be imported into the workspace.
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
 * ## How do I control the details?
 * 
 * See {@link OomphIdeExtension} for the full DSL.
 * 
 * ## Proxy support
 * 
 * If you need to pass through a proxy, you'll need to create a file
 * called `proxies.ini` with content like this:
 * 
 * ```ini
 * org.eclipse.core.net/proxyData/HTTP/host=someproxy.ericsson.se
 * org.eclipse.core.net/proxyData/HTTPS/host=someproxy.ericsson.se
 * org.eclipse.core.net/proxyData/HTTPS/hasAuth=false
 * org.eclipse.core.net/proxyData/HTTP/port=8080
 * org.eclipse.core.net/proxyData/HTTPS/port=8080
 * org.eclipse.core.net/org.eclipse.core.net.hasMigrated=true
 * org.eclipse.core.net/nonProxiedHosts=*.ericsson.com|127.0.0.1
 * org.eclipse.core.net/systemProxiesEnabled=false
 * org.eclipse.core.net/proxyData/HTTP/hasAuth=false 
 * ```
 * 
 * Once you've done this, add this to your `build.gradle`:
 * 
 * ```groovy
 * oomphIde {
 *     ...
 * 
 *     p2director {
 *         addArg('plugincustomization', '<path to proxies.ini>')
 *     }
 * }
 * ```
 * 
 * If you think this is too hard, vote for [this issue on GitHub](https://github.com/diffplug/goomph/issues/12)
 * and [this bug on eclipse](https://bugs.eclipse.org/bugs/show_bug.cgi?id=382875) and we can make it easier.
 * 
 */
public class OomphIdePlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		OomphIdeExtension extension = project.getExtensions().create(OomphIdeExtension.NAME, OomphIdeExtension.class, project);
		// ideSetupP2
		Task ideSetupP2 = project.getTasks().create(IDE_SETUP_P2);
		ideSetupP2.doFirst(unused -> {
			Errors.rethrow().run(extension::ideSetupP2);
		});
		// ideSetupWorkspace
		Task ideSetupWorkspace = project.getTasks().create(IDE_SETUP_WORKSPACE);
		ideSetupWorkspace.doFirst(unused -> {
			Errors.rethrow().run(extension::ideSetupWorkspace);
		});
		ideSetupWorkspace.dependsOn(ideSetupP2);
		// ide
		Task ide = project.getTasks().create(IDE);
		ide.doFirst(unused -> {
			Errors.rethrow().run(extension::ide);
		});

		project.afterEvaluate(p -> {
			Errors.rethrow().run(() -> {
				// ideSetup -> eclipse
				extension.addDependency(p);
				// tie ide to ideSetupP2 iff setup is required
				if (!extension.p2isClean()) {
					ide.dependsOn(ideSetupP2);
				}
				// tie ide to ideSetupWorkspace if there's no workspace
				if (!extension.workspaceExists()) {
					ide.dependsOn(ideSetupWorkspace);
				}
			});
		});

		// ideClean
		Task ideClean = project.getTasks().create(IDE_CLEAN);
		ideClean.doFirst(unused -> {
			extension.ideClean();
		});
		ideSetupP2.mustRunAfter(ideClean);
		ideSetupWorkspace.mustRunAfter(ideClean);
	}

	static final String IDE = "ide";
	static final String IDE_SETUP_WORKSPACE = "ideSetupWorkspace";
	static final String IDE_SETUP_P2 = "ideSetupP2";
	static final String IDE_CLEAN = "ideClean";
}
