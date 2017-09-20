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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.p2.ParsedJar;

public class EclipseLauncherTask extends DefaultTask {
	private List<String> extraDeps = new ArrayList<>();

	private Project root;

	private File output;

	private File workingDir;

	private List<String> args;

	public List<String> getExtraDeps() {
		return extraDeps;
	}

	public void setExtraDeps(List<String> extraDeps) {
		this.extraDeps = extraDeps;
	}

	public Project getRoot() {
		return root;
	}

	public void setRoot(Project root) {
		this.root = root;
	}

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	@TaskAction
	public void run() throws Exception {
		Set<File> projectDeps = root.getConfigurations()
					.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME)
					.resolve();

		List<File> extDeps = new ArrayList<>(extraDeps.size() + 1);

		Jar jarTask = (Jar) root.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
		extDeps.add(jarTask.getArchivePath());

		for (String mavenCoord : extraDeps) {
			Dependency dep = getProject().getDependencies().create(mavenCoord);
			extDeps.addAll(
			getProject().getConfigurations().detachedConfiguration(dep)
			.setTransitive(false)
			.resolve());
		}

		Set<File> plugins = new HashSet<>(projectDeps.size() + extDeps.size());
		plugins.addAll(projectDeps);
		plugins.addAll(extDeps);

		FileMisc.cleanDir(output);
		File pluginsDir = new File(output, "plugins");
		pluginsDir.mkdirs();

		for (File plugin : plugins) {
			ParsedJar parsed = new ParsedJar(plugin);
			String name = parsed.getSymbolicName() + "_" + parsed.getVersion() + ".jar";
			Files.copy(plugin, new File(pluginsDir, name));
		}

		JarFolderRunnerExternalJvm toRun = new JarFolderRunnerExternalJvm(output, getProject());
		toRun.run(args);
	}

	private static final Logger logger = Logger.getLogger(EclipseLauncherTask.class.getName());
}
