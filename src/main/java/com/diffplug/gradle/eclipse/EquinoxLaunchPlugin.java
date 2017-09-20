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
package com.diffplug.gradle.eclipse;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;

import com.diffplug.gradle.ProjectPlugin;

/**
 * Plugin for launching an equinox runtime.
 * 
 * 
 * Quick example (from 
 * 
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.equinoxlaunch'
 * equinoxLaunch {
 *     myEquinox {
 *         // adds the given project and all its runtime deps to the runtime
 *         src.addProject(project(':myproject'))
 *         // adds a specific configuration to the runtime
 *         src.addConfiguration(configurations.runtime)
 *         // adds a lone maven artifact, without any of its transitives
 *         src.addMaven('com.google.guava:guava:17.0')
 *         src.addMaven('com.google.guava:guava:18.0')
 *
 *         // optional argument, default is below 
 *         installDir = 'build/equinoxLaunchMyEquinox
 *
 *         launch 'myApp', {
 *             // optional argument, default is project directory
 *             workingDir = file('myWorkingDir')
 *             args = ['-consoleLog', '-application', 'com.myapp']
 *         }
 *     }
 * }
 */
public class EquinoxLaunchPlugin extends ProjectPlugin {
	public static final String NAME = "equinoxLaunch";

	@Override
	protected void applyOnce(Project project) {
		NamedDomainObjectContainer<EquinoxLaunchConfig> container = project.container(EquinoxLaunchConfig.class, new NamedDomainObjectFactory<EquinoxLaunchConfig>() {
			@Override
			public EquinoxLaunchConfig create(String name) {
				return new EquinoxLaunchConfig(project, name);
			}
		});
		project.getExtensions().add(NAME, container);

		project.afterEvaluate(unused -> {
			container.all(EquinoxLaunchConfig::createTasks);
		});
	}
}
