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
import java.util.List;
import java.util.Objects;

import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.eclipserunner.EclipseIniLauncher;
import com.diffplug.gradle.osgi.OsgiExecable;

/**
 * Runs a series of actions with the OSGi context.
 *
 * It's highly recommended to use {@link OsgiExecable.ReflectionHost}
 * so that we can compile against the eclipse code within Goomph,
 * and then run it against the OSGi runtime.
 */
class SetupWithinEclipse implements JavaExecable {
	private static final long serialVersionUID = -7563836594137010936L;

	File eclipseRoot;
	List<SetupAction> actionsWithinEclipse;

	public SetupWithinEclipse(File eclipseRoot, List<SetupAction> list) {
		this.eclipseRoot = Objects.requireNonNull(eclipseRoot);
		this.actionsWithinEclipse = Objects.requireNonNull(list);
	}

	@Override
	public void run() throws Throwable {
		EclipseIniLauncher launcher = new EclipseIniLauncher(eclipseRoot);
		try (EclipseIniLauncher.Running running = launcher.open()) {
			// run the plugins
			System.out.println("Running internal setup actions...");
			for (SetupAction action : actionsWithinEclipse) {
				System.out.print("    " + action.getDescription() + "... ");
				OsgiExecable.exec(running.bundleContext(), action);
				System.out.println("done.");
			}
			System.out.println("Internal setup complete.");
		}
	}
}
