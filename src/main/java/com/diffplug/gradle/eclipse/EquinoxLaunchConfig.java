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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.Project;

import com.diffplug.common.base.Preconditions;

public class EquinoxLaunchConfig {
	private final String name;
	private final Project project;
	private final EquinoxLaunchSource src;
	private File installDir;
	private final Map<String, Action<EquinoxLaunchTask>> runConfigs = new HashMap<>();

	EquinoxLaunchConfig(Project project, String name) {
		this.project = project;
		this.name = capitalize(name);
		this.src = new EquinoxLaunchSource(project);
		this.installDir = new File(project.getBuildDir(), EquinoxLaunchPlugin.NAME + this.name);
	}

	public EquinoxLaunchSource getSrc() {
		return src;
	}

	public File getInstallDir() {
		return installDir;
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}

	public void launchTask(String name, Action<EquinoxLaunchTask> config) {
		runConfigs.put(name, config);
	}

	void createTasks() {
		EquinoxLaunchSetupTask setupTask = project.getTasks().create(EquinoxLaunchPlugin.NAME + name + "Setup", EquinoxLaunchSetupTask.class);
		setupTask.setInstallDir(installDir);
		setupTask.setSource(src);

		runConfigs.forEach((name, configAction) -> {
			EquinoxLaunchTask launchTask = project.getTasks().create(name, EquinoxLaunchTask.class);
			launchTask.dependsOn(setupTask);
			launchTask.setInstallDir(installDir);
			launchTask.setWorkingDir(project.getProjectDir());
			launchTask.setArgs(new ArrayList<>());
			configAction.execute(launchTask);
		});
	}

	private static String capitalize(String name) {
		Preconditions.checkArgument(!name.isEmpty(), "Can't be empty");
		return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
	}
}
