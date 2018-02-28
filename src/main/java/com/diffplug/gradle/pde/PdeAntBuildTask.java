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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Preconditions;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.eclipserunner.EclipseApp;
import com.diffplug.gradle.oomph.WorkspaceRegistry;

/**
 * Runs PDE build on an ant file.
 * 
 * Your project must have defined `GOOMPH_PDE_VER`, see
 * {@link PdeInstallation#fromProject(org.gradle.api.Project)}
 * for details.
 * 
 * ```groovy
 * task featureBuild(type: PdeAntBuildTask) {
 *     antFile(FEATURE + '.xml')
 *     define('featuredir', FEATURE)
 *     inputs.dir(FEATURE)
 *     defineToFile('repodir', buildDir)
 *     outputs.dir(buildDir)
 * }
 * ```
 */
public class PdeAntBuildTask extends DefaultTask {
	WorkspaceRegistry registry;

	public PdeAntBuildTask() {
		try {
			this.registry = WorkspaceRegistry.instance();
		} catch (IOException e) {
			getLogger().warn("WorkspaceRegistry could not be instantiated! Using default workspace.", e);
			this.registry = null;
		}
	}

	private Object antFile;

	/** The directory from which plugins will be pulled, besides the delta pack. */
	public void antFile(Object antFile) {
		this.antFile = antFile;
	}

	private Map<String, String> buildProperties = new LinkedHashMap<>();

	/** Adds a property to the build properties file. */
	public void define(String key, String value) {
		buildProperties.put(key, value);
	}

	/** Adds a property to the build properties file. */
	public void defineToFile(String key, Object value) {
		buildProperties.put(key, getProject().file(value).getAbsolutePath());
	}

	@TaskAction
	public void build() throws Exception {
		Preconditions.checkNotNull(antFile, "antFile must not be null!");
		EclipseApp antRunner = new EclipseApp(EclipseApp.AntRunner.ID);
		antRunner.addArg("buildfile", getProject().file(antFile).getAbsolutePath());
		buildProperties.forEach((key, value) -> {
			antRunner.addArg("D" + key + "=" + FileMisc.quote(value));
		});
		final PdeInstallation installation = PdeInstallation.fromProject(getProject());
		if (registry != null) {
			installation.setWorkspaceProvider(i -> registry.workspaceDir(getProject(), this));
		}
		antRunner.runUsing(installation);
	}
}
