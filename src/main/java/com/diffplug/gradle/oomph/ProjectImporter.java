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

	/** Launch the eclipse instance that we have installed. */
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

	/**
	 * Launch the project importer within that eclipse.  Use reflection to ensure
	 * that we don't have to have the eclipse jars as dependencies, since we
	 * can get them inside the OSGi container.
	 */
	static class Osgi extends OsgiExecable.ReflectionHost {
		private static final long serialVersionUID = 6542985814638851088L;

		ArrayList<File> projects;

		public Osgi(ArrayList<File> projects) {
			super("com.diffplug.gradle.oomph.ProjectImporterInternal");
			this.projects = Objects.requireNonNull(projects);
		}
	}
}
