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
package com.diffplug.gradle.eclipserunner;

import java.io.File;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

import com.diffplug.gradle.ProjectPlugin;

/**
 * Plugin for launching an equinox runtime.
 * 
 * Here is a simple example:
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.equinoxlaunch'
 * equinoxLaunch {
 *     // creates an EquinoxLaunchSetupTask named 'headlessAppSetup'
 *     headlessAppSetup {
 *         source.addThisProject()
 *         source.addMaven('com.google.guava:guava:17.0')
 *         source.addMaven('com.google.guava:guava:18.0')
 *         // creates an EquinoxLaunchTask named 'headlessApp' which depends on 'headlessAppSetup'
 *         launchTask 'headlessApp', {
 *             it.args = ['-consoleLog', '-application', 'com.diffplug.rcpdemo.headlessapp', 'file', 'test']
 *         }
 *     }
 * }
 * ```
 * 
 * And a more complex example:
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.equinoxlaunch'
 * equinoxLaunch {
 *     // creates an EquinoxLaunchSetupTask named 'equinoxSetup'
 *     equinoxSetup {
 *         // adds the given project and all its runtime deps to the runtime
 *         source.addProject(project(':myproject'))
 *         // adds a specific configuration to the runtime
 *         source.addConfiguration(configurations.runtime)
 *         // adds a lone maven artifact, without any of its transitives
 *         source.addMaven('com.google.guava:guava:17.0')
 *         source.addMaven('com.google.guava:guava:18.0')
 *
 *         // optional argument, default is "build/<setupTaskName>" 
 *         installDir = 'build/equinoxSetup'
 *
 *         // creates an EquinoxLaunchTask named 'launchApp' which depends on 'equinoxSetup'
 *         launchTask 'launchApp', {
 *             // optional argument, default is project directory
 *             workingDir = file('myWorkingDir')
 *             // -consoleLog is strongly recommended for debugging
 *             args = ['-consoleLog', '-application', 'com.myapp']
 *         }

 *         // creates an EquinoxLaunchTask named 'launchApp2' which depends on 'equinoxSetup'
 *         launchTask 'launchApp2', {
 *             // optional argument, default is project directory
 *             workingDir = file('myWorkingDir')
 *             // -consoleLog is strongly recommended for debugging
 *             args = ['-consoleLog', '-application', 'com.myotherapp', 'other', 'args']
 *         }
 *     }
 * }
 */
public class EquinoxLaunchPlugin extends ProjectPlugin {
	public static final String NAME = "equinoxLaunch";

	@Override
	protected void applyOnce(Project project) {
		project.getExtensions().add(NAME, project.container(EquinoxLaunchSetupTask.class, new NamedDomainObjectFactory<EquinoxLaunchSetupTask>() {
			@Override
			public EquinoxLaunchSetupTask create(String name) {
				EquinoxLaunchSetupTask setupTask = project.getTasks().create(name, EquinoxLaunchSetupTask.class);
				setupTask.setInstallDir(new File(project.getBuildDir(), name));
				return setupTask;
			}
		}));
	}
}
