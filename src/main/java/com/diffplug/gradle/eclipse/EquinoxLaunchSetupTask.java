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
import java.io.IOException;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.io.Files;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.p2.ParsedJar;

public class EquinoxLaunchSetupTask extends DefaultTask {
	@Input
	EquinoxLaunchSource source;

	@OutputDirectory
	File installDir;

	@TaskAction
	public void copyFiles() throws IOException {
		FileMisc.cleanDir(installDir);
		File pluginsDir = new File(installDir, "plugins");
		pluginsDir.mkdirs();

		for (File plugin : source.resolvedFiles()) {
			ParsedJar parsed = new ParsedJar(plugin);
			String name = parsed.getSymbolicName() + "_" + parsed.getVersion() + ".jar";
			Files.copy(plugin, new File(pluginsDir, name));
		}
	}

	////////////////////////////////////////
	// Auto-generated getters and setters //
	////////////////////////////////////////
	public EquinoxLaunchSource getSource() {
		return source;
	}

	public void setSource(EquinoxLaunchSource source) {
		this.source = source;
	}

	public File getInstallDir() {
		return installDir;
	}

	public void setInstallDir(File installDir) {
		this.installDir = installDir;
	}
}
