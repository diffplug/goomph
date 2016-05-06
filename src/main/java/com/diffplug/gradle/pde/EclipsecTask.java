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
package com.diffplug.gradle.pde;

import java.io.File;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import com.diffplug.gradle.CmdLine;

/**
 * A Gradle task which runs an `eclipsec` command at the console.
 * 
 * Probably not very useful in isolation, but it is used by
 * {@link P2DirectorModel} to run P2 tasks, so we'll keep it around.
 * 
 * WARNING: This part of Goomph currently has the following precondition:
 * your project must have the property VER_ECLIPSE=4.5.2 (or some other version),
 * and you must have installed that Eclipse using Wuff. We will remove this
 * restriction in the future.
 * 
 * ```groovy
 * task runP2director(type: EclipseTask) {
 *     addArg('nosplash', '')
 *     addArg('application', '')
 *     addArg('application', 'org.eclipse.equinox.p2.director')
 * }
 * ```
 * 
 * will run `eclipsec -nosplash -application org.eclipse.equinox.p2.director`.
 */
public class EclipsecTask extends DefaultTask {
	private ListMultimap<String, String> args = ArrayListMultimap.create();

	/** Adds a console argument. */
	public void addArg(String key, String value) {
		args.put(key, value);
	}

	@TaskAction
	public void build() throws Exception {
		EclipseWuff eclipse = new EclipseWuff(getProject());

		StringBuilder builder = new StringBuilder();
		builder.append(quote(eclipse.getEclipseConsoleExecutable()));
		for (String key : args.keySet()) {
			builder.append(" -");
			builder.append(key);
			builder.append(" ");
			builder.append(Joiner.on(",").join(args.get(key)));
		}
		// execute the cmd
		CmdLine.runCmd(builder.toString());
	}

	/** Returns the absolute path quoted. */
	private static String quote(File file) {
		return "\"" + file.getAbsolutePath() + "\"";
	}
}
