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
package com.diffplug.gradle.oomph;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.eclipserunner.EclipseIniLauncher;
import com.diffplug.gradle.eclipserunner.osgiembed.OsgiExecable;

/** Imports projects into the workspace. */
class ProjectImporter {
	public static void execute(File eclipseRoot, ArrayList<File> projects) {
		Errors.rethrow().run(() -> {
			JavaExecable.execWithoutGradle(new Java(eclipseRoot, projects));
		});
	}

	private static class Java implements JavaExecable {
		private static final long serialVersionUID = -7563836594137010936L;

		File eclipseRoot;
		ArrayList<File> projects;

		public Java(File eclipseRoot, ArrayList<File> projects) {
			this.eclipseRoot = Objects.requireNonNull(eclipseRoot);
			this.projects = Objects.requireNonNull(projects);
		}

		@Override
		public void run() throws Throwable {
			EclipseIniLauncher launcher = new EclipseIniLauncher(eclipseRoot);
			try (EclipseIniLauncher.Running running = launcher.open()) {
				OsgiExecable.exec(running.bundleContext(), new Osgi(projects));
			}
		}
	}

	private static class Osgi implements OsgiExecable {
		private static final long serialVersionUID = 6542985814638851088L;

		ArrayList<File> projects;

		public Osgi(ArrayList<File> projects) {
			this.projects = Objects.requireNonNull(projects);
		}

		@Override
		public void run() {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			for (File projectFile : projects) {
				try {
					Path path = new Path(projectFile.toString());
					IProjectDescription description = workspace.loadProjectDescription(path);

					IProject project = workspace.getRoot().getProject(description.getName());
					if (project.isOpen() == false) {
						project.create(description, null);
						project.open(null);
					} else {
						project.refreshLocal(IResource.DEPTH_INFINITE, null);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
