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

import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.diffplug.gradle.CmdLine;

public class PdeAntBuildTask extends DefaultTask {
	private Object antFile;

	/** The directory from which plugins will be pulled, besides the delta pack. */
	public void antFile(Object antFile) {
		this.antFile = antFile;
	}

	private Map<String, String> buildProperties = Maps.newLinkedHashMap();

	/** Adds a property to the build properties file. */
	public void addProperty(String key, String value) {
		buildProperties.put(key, value);
	}

	/** Adds a property to the build properties file. */
	public void addFileProperty(String key, Object value) {
		buildProperties.put(key, getProject().file(value).getAbsolutePath());
	}

	@TaskAction
	public void build() throws Exception {
		Preconditions.checkNotNull(antFile, "antFile must not be null!");

		// generate and execute the PDE build command
		PdeBuild pdeBuild = new PdeBuild(getProject());
		StringBuilder cmd = new StringBuilder();
		cmd.append(pdeBuild.antBuildCmd(getProject().file(antFile)));
		for (Map.Entry<String, String> entry : buildProperties.entrySet()) {
			cmd.append(" -D");
			cmd.append(entry.getKey());
			cmd.append("=");
			cmd.append(entry.getValue());
		}
		CmdLine.runCmd(cmd.toString());
	}
}
