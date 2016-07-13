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
package com.diffplug.gradle.p2;

import java.io.File;

import org.gradle.api.Project;

import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.eclipserunner.EclipseApp;

/** Models the repo2runnable application. */
public class Repo2Runnable extends EclipseApp {
	public Repo2Runnable() {
		super("org.eclipse.equinox.p2.repository.repo2runnable");
	}

	/** Adds the given location as a source. */
	public void source(File file) {
		addArg("source", FileMisc.asUrl(file));
	}

	/** Adds the given location as a destination. */
	public void destination(File file) {
		addArg("destination", FileMisc.asUrl(file));
	}

	/** Runs this application, downloading a small bootstrapper if necessary. */
	public void runUsingBootstrapper() throws Exception {
		runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
	}

	/** Runs this application, downloading a small bootstrapper if necessary. */
	public void runUsingBootstrapper(Project project) throws Exception {
		runUsing(P2BootstrapInstallation.latest().outsideJvmRunner(project));
	}
}
