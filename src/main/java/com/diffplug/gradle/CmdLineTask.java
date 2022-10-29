/*
 * Copyright (C) 2015-2022 DiffPlug
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
package com.diffplug.gradle;


import groovy.lang.Closure;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * A task for running a series of actions, including
 * shell commands.
 * 
 * ```groovy
 * task mirrorRepo(type: com.diffplug.gradle.CmdLineTask) {
 *     // initialize the repository
 *     cmd("svnadmin create ${name}")
 *     // setup its script
 *     copy(SCRIPT, "${name}/hooks/${SCRIPT}")
 *     // initialize the sync
 *     cmd("svnsync initialize file:///${project.file(name).absolutePath} ${url}")
 *     // follow throw on the sync
 *     cmd("svnsync synchronize file:///${project.file(name).absolutePath} ${url}")
 * }
 * ```
 **/
public class CmdLineTask extends DefaultTask {
	private CmdLine cmdLine = new CmdLine();

	public void cleanDir(Object dir) {
		cmdLine.cleanDir(getProject().file(dir));
	}

	/** Executes the given command. */
	public void cmd(String cmd) {
		cmdLine.cmd(getProject().getProjectDir(), cmd);
	}

	/** Sets the working directory to the given dir, then executes the given command. */
	public void cmd(Object directory, String cmd) {
		cmdLine.cmd(getProject().file(directory), cmd);
	}

	/** Removes the given file or directory. */
	public void rm(Object fileOrDir) {
		cmdLine.rm(getProject().file(fileOrDir));
	}

	/** Removes the given file or directory. */
	public void copy(Object src, Object dst) {
		cmdLine.copy(getProject().file(src), getProject().file(dst));
	}

	/** Removes the given file or directory. */
	public void mv(Object src, Object dst) {
		cmdLine.mv(getProject().file(src), getProject().file(dst));
	}

	/** Removes the given file or directory. */
	public void run(Closure<?> action) {
		cmdLine.run(action);
	}

	@TaskAction
	public void performActions() throws Throwable {
		cmdLine.performActions();
	}
}
