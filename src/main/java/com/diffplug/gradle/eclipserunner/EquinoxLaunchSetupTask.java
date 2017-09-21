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
import java.io.IOException;
import java.util.ArrayList;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.io.Files;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.p2.ParsedJar;

public class EquinoxLaunchSetupTask extends DefaultTask {
	@Input
	final EquinoxLaunchSource source;

	@OutputDirectory
	File installDir;

	public EquinoxLaunchSetupTask() {
		source = new EquinoxLaunchSource(this);
	}

	@TaskAction
	public void copyFiles() throws IOException {
		FileMisc.cleanDir(installDir);
		File pluginsDir = new File(installDir, "plugins");
		FileMisc.mkdirs(pluginsDir);

		for (File plugin : source.resolvedFiles()) {
			ParsedJar parsed = new ParsedJar(plugin);
			String name = parsed.getSymbolicName() + "_" + parsed.getVersion() + ".jar";
			Files.copy(plugin, new File(pluginsDir, name));
		}
	}

	/** Creates a launch task which depends on this SetupTask. */
	public EquinoxLaunchTask launchTask(String name) {
		EquinoxLaunchTask launchTask = getProject().getTasks().create(name, EquinoxLaunchTask.class);
		launchTask.dependsOn(this);
		launchTask.setInstallDir(installDir);
		launchTask.setWorkingDir(getProject().getProjectDir());
		launchTask.setArgs(new ArrayList<>());
		return launchTask;
	}

	/** Creates a launch task which depends on this SetupTask. */
	public EquinoxLaunchTask launchTask(String name, Action<EquinoxLaunchTask> configure) {
		EquinoxLaunchTask launchTask = launchTask(name);
		configure.execute(launchTask);
		return launchTask;
	}

	////////////////////////////////////////
	// Auto-generated getters and setters //
	////////////////////////////////////////
	public EquinoxLaunchSource getSource() {
		return source;
	}

	public File getInstallDir() {
		return installDir;
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}
}
