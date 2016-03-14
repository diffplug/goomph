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
package com.diffplug.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Throwing;

/** A task for running command line actions which may have multiple lines. */
public class CmdLineTask extends DefaultTask {
	private CmdLine cmdLine = new CmdLine();

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
	public void run(Throwing.Runnable action) {
		cmdLine.run(action);
	}

	@TaskAction
	public void performActions() throws Throwable {
		cmdLine.performActions();
	}
}
