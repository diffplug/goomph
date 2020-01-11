/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.eclipserunner;


import java.io.File;
import java.util.List;
import java.util.Objects;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Launches an equinox application based on a plugin setup
 * established by {@link EquinoxLaunchSetupTask}.
 */
public class EquinoxLaunchTask extends DefaultTask {
	private File installDir;
	private File workingDir;
	private List<String> args;
	private List<String> vmArgs;

	@TaskAction
	public void launch() throws Exception {
		Objects.requireNonNull(installDir, "installDir");
		// workingDir can be null
		Objects.requireNonNull(args, "args");
		JarFolderRunnerExternalJvm jvm = new JarFolderRunnerExternalJvm(installDir, workingDir, getProject());
		jvm.setVmArgs(vmArgs);
		jvm.run(args);
	}

	////////////////////////////////////////
	// Auto-generated getters and setters //
	////////////////////////////////////////
	@InputDirectory
	public File getInstallDir() {
		return installDir;
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}

	@Internal
	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	@Input
	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	@Input
	public List<String> getVmArgs() {
		return vmArgs;
	}

	public void setVmArgs(List<String> vmArgs) {
		this.vmArgs = vmArgs;
	}
}
