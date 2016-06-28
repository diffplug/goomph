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

import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.eclipserunner.EclipseIniLauncher;
import com.diffplug.gradle.eclipserunner.osgiembed.OsgiExecable;

/**
 * Runs a series of actions with the OSGi context.
 *
 * Uses {@link OsgiExecable.ReflectionHost} so that we can
 * compile the code against the eclipse code within Goomph,
 * and then run it against the OSGi runtime.
 */
class SetupWithinEclipse implements JavaExecable {
	private static final long serialVersionUID = -7563836594137010936L;

	File eclipseRoot;
	ArrayList<OsgiExecable.ReflectionHost> actionsWithinEclipse = new ArrayList<>();

	public SetupWithinEclipse(File eclipseRoot) {
		this.eclipseRoot = Objects.requireNonNull(eclipseRoot);
	}

	public void add(OsgiExecable.ReflectionHost action) {
		actionsWithinEclipse.add(action);
	}

	@Override
	public void run() throws Throwable {
		EclipseIniLauncher launcher = new EclipseIniLauncher(eclipseRoot);
		try (EclipseIniLauncher.Running running = launcher.open()) {
			for (OsgiExecable.ReflectionHost host : actionsWithinEclipse) {
				OsgiExecable.exec(running.bundleContext(), host);
			}
		}
	}
}
