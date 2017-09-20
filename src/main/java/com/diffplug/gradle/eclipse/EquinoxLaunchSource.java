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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;

/** Defines the source jars for an eclipse launch, with lazy resolution for performance. */
public class EquinoxLaunchSource extends LazyForwardingEquality<FileSignature> {
	private static final long serialVersionUID = 3644633962761683264L;

	public EquinoxLaunchSource(Project anchorProject) {
		this.anchorProject = anchorProject;
	}

	/** Used to resolve extraDeps. */
	transient final Project anchorProject;
	transient List<Configuration> configurations = new ArrayList<>();
	transient List<String> extraDeps = new ArrayList<>();

	/** Adds the runtime and generated archives for this project. */
	public void addThisProject() {
		addProject(anchorProject);
	}

	/** Adds the runtime and generated archives for the given project. */
	public void addProject(Project project) {
		addConfiguration(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME));
		addConfiguration(project.getConfigurations().getByName(Dependency.ARCHIVES_CONFIGURATION));
	}

	/** Adds the given configuration. */
	public void addConfiguration(Configuration config) {
		configurations.add(config);
	}

	/** Adds a lone maven artifact, without any of its transitives. */
	public void addMaven(String mavenCoord) {
		extraDeps.add(mavenCoord);
	}

	@Override
	protected FileSignature calculateState() throws Exception {
		Set<File> files = new LinkedHashSet<>();
		for (Configuration configuration : configurations) {
			files.addAll(configuration.resolve());
		}
		for (String extraDep : extraDeps) {
			Dependency dep = anchorProject.getDependencies().create(extraDep);
			files.addAll(anchorProject.getConfigurations()
					.detachedConfiguration(dep)
					.setDescription(extraDep)
					.setTransitive(false)
					.resolve());
		}
		return FileSignature.signAsList(files);
	}

	/** Returns the files which were resolved. */
	public Collection<File> resolvedFiles() {
		return state().files();
	}
}
