/*
 * Copyright (C) 2017-2024 DiffPlug
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

import com.diffplug.common.base.Unhandled;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.LazyForwardingEquality;
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
import org.gradle.api.tasks.bundling.Jar;

/** Defines the source jars for an eclipse launch, with lazy resolution for performance. */
public class EquinoxLaunchSource extends LazyForwardingEquality<FileSignature> {
	private static final long serialVersionUID = 3644633962761683264L;

	EquinoxLaunchSource(EquinoxLaunchSetupTask setupTask) {
		this.setupTask = setupTask;
	}

	/** Used to resolve extraDeps. */
	transient final EquinoxLaunchSetupTask setupTask;
	transient List<Object> projConfigMaven = new ArrayList<>();

	/** Adds the runtime and generated archives for this project. */
	public void addThisProject() {
		addProject(setupTask.getProject());
	}

	/** Adds the runtime and jar archive for the given project. */
	public void addProject(Project project) {
		Jar jar = taskFor(project);
		setupTask.dependsOn(jar);
		projConfigMaven.add(project);
	}

	/** Adds the given configuration. */
	public void addConfiguration(Configuration config) {
		projConfigMaven.add(config);
	}

	/** Adds a lone maven artifact, without any of its transitives. */
	public void addMaven(String mavenCoord) {
		projConfigMaven.add(mavenCoord);
	}

	private static Jar taskFor(Project project) {
		return (Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME);
	}

	@Override
	protected FileSignature calculateState() throws Exception {
		Set<File> files = new LinkedHashSet<>();
		for (Object o : projConfigMaven) {
			if (o instanceof Project) {
				Project project = (Project) o;
				Jar jar = taskFor(project);
				files.add(jar.getArchiveFile().get().getAsFile());
				files.addAll(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).resolve());
			} else if (o instanceof Configuration) {
				Configuration config = (Configuration) o;
				files.addAll(config.resolve());
			} else if (o instanceof String) {
				String mavenCoord = (String) o;
				Dependency dep = setupTask.getProject().getDependencies().create(mavenCoord);
				files.addAll(setupTask.getProject().getConfigurations()
						.detachedConfiguration(dep)
						.setDescription(mavenCoord)
						.setTransitive(false)
						.resolve());
			} else {
				throw Unhandled.classException(o);
			}
		}
		return FileSignature.from(files);
	}

	/** Returns the files which were resolved. */
	public Collection<File> resolvedFiles() {
		return state().files();
	}
}
