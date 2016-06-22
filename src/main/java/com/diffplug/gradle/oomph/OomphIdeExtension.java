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
import java.util.Objects;

import org.gradle.api.Action;
import org.gradle.api.Project;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.p2.P2Model;

/** DSL for {@link OomphIdePlugin}. */
public class OomphIdeExtension {
	public static final String NAME = "oomphIde";

	private final Project project;

	OomphIdeExtension(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	private final P2Model p2 = new P2Model();

	/** Returns the P2 model so that users can add the features they'd like. */
	public P2Model getP2() {
		return p2;
	}

	private Action<OomphTargetPlatform> targetPlatform = null;

	/** Sets the targetplatform configuration. */
	public void targetPlatform(Action<OomphTargetPlatform> targetPlatform) {
		Preconditions.checkArgument(this.targetPlatform == null, "Can only set targetplatform once");
		this.targetPlatform = targetPlatform;
	}

	private Object ideDir = "build/oomph-ide";

	/** Sets the folder where the ide will be built. */
	public void ideDir(Object ideDir) {
		this.ideDir = ideDir;
	}

	private File getIdeDir() {
		return project.file(ideDir);
	}

	private String state() {
		return getIdeDir().toString() + Objects.toString(targetPlatform) + p2;
	}

	static final String STALE_TOKEN = "token_stale";

	/** Sets up an IDE as described in this model from scratch. */
	void setup() throws Exception {
		// if we've got a clean token, we're all good
		if (FileMisc.hasToken(getIdeDir(), STALE_TOKEN, state())) {
			return;
		}
		// else we've gotta set it up
		FileMisc.cleanDir(getIdeDir());
		// now we can install the IDE
		p2.addArtifactRepoBundlePool();
		P2Model.DirectorApp app = p2.directorApp(getIdeDir(), "OomphIde");
		app.consolelog();
		// share the install for quickness
		app.bundlepool(GoomphCacheLocations.bundlePool());
		// create the native launcher
		app.platform(SwtPlatform.getRunning());
		// create it
		app.runUsingBootstrapper(project);
		// TODO: setup workspace
		if (targetPlatform != null) {
			OomphTargetPlatform targetPlatformInstance = new OomphTargetPlatform(project);
			targetPlatform.execute(targetPlatformInstance);
			// TODO: setup targetplatform in workspace
		}
	}

	/** Runs the IDE which was setup by {@link #setup()}. */
	void run() {

	}
}
