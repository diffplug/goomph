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
import java.util.Objects;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.internal.Actions;

import com.diffplug.gradle.FileMisc;

/** Implementation of the p2 -> maven conversion. */
class AsMaven {
	final Project project;

	public AsMaven(Project project) {
		this.project = project;
	}

	/** Returns the destination directory for the p2 repository. */
	public File getDestinationP2() {
		return new File(project.file(destination), SUBDIR_P2);
	}

	/** Returns the destination directory for the p2 repository. */
	public File getDestinationP2Runnable() {
		return new File(project.file(destination), SUBDIR_P2_RUNNABLE);
	}

	/** Returns the destination directory for the maven repository. */
	public File getDestinationMaven() {
		return new File(project.file(destination), SUBDIR_MAVEN);
	}

	private static final String SUBDIR_P2 = "p2";
	private static final String SUBDIR_P2_RUNNABLE = "p2runnable";
	private static final String SUBDIR_MAVEN = "maven";

	public void run() throws Exception {
		Objects.requireNonNull(mavenGroup, "Must set mavengroup");
		// record the user's inputs
		String state = state();

		// if we've already written a token which confirms we're done with these inputs, then bail
		File dir = project.file(destination);
		if (FileMisc.hasToken(dir, STALE_TOKEN, state)) {
			project.getLogger().debug("p2AsMaven " + mavenGroup + " is satisfied");
			return;
		} else {
			project.getLogger().lifecycle("p2AsMaven " + mavenGroup + " is dirty.");
		}
		// else, we'll need to run our own little thing
		FileMisc.cleanDir(dir);
		File p2dir = new File(dir, SUBDIR_P2);
		File mavenDir = new File(dir, SUBDIR_MAVEN);

		// install from p2
		project.getLogger().lifecycle("Initalizing maven group " + mavenGroup + " from p2");
		project.getLogger().lifecycle("Only needs to be done once, future builds will be much faster");

		FileMisc.mkdirs(p2dir);
		project.getLogger().lifecycle("p2AsMaven " + mavenGroup + " installing from p2");
		getApp().runUsingBootstrapper(project);

		if (repo2runnable) {
			project.getLogger().lifecycle("p2AsMaven " + mavenGroup + " creating runnable repo");
			Repo2Runnable app = new Repo2Runnable();
			app.source(getDestinationP2());
			app.destination(getDestinationP2Runnable());
			app.runUsingBootstrapper(project);
		}

		// put p2 into a maven repo
		project.getLogger().lifecycle("p2AsMaven " + mavenGroup + " creating maven repo");
		FileMisc.mkdirs(mavenDir);
		try (MavenRepoBuilder maven = new MavenRepoBuilder(mavenDir)) {
			for (File plugin : FileMisc.list(new File(p2dir, "plugins"))) {
				if (plugin.isFile() && plugin.getName().endsWith(".jar")) {
					maven.install(mavenGroup, plugin);
				}
			}
		}

		// write out the staleness token to indicate that everything is good
		FileMisc.writeToken(dir, STALE_TOKEN, state);
		project.getLogger().lifecycle("p2AsMaven " + mavenGroup + " is complete.");
	}

	static final String STALE_TOKEN = "stale_token";

	/** The args passed to p2 director represent the full state. */
	private String state() {
		return "mirrorApp: " + getApp().completeState() + "\nmavenGroup: " + mavenGroup + "\ngoomph:" + GOOMPH_VERSION + "\nrepo2runnable:" + repo2runnable;
	}

	/** Bump this if we need to force people's deps to reload. */
	static final int GOOMPH_VERSION = 1;

	private P2Model.MirrorApp getApp() {
		P2Model.MirrorApp app = p2model.mirrorApp(getDestinationP2());
		modifyAnt.execute(app);
		return app;
	}

	public void mavenGroup(String mavenGroup) {
		this.mavenGroup = mavenGroup;
	}

	public void destination(Object destination) {
		this.destination = destination;
	}

	public P2Model p2() {
		return p2model;
	}

	public void modifyAntTask(Action<P2Model.MirrorApp> args) {
		this.modifyAnt = Objects.requireNonNull(args);
	}

	/** The group which will be used in the maven-ization. */
	private String mavenGroup;
	/** When this is true, the global bundle pool will be used to accelerate artifact downloads. */
	private Object destination = "build/p2asmaven";
	/** The model we'd like to download. */
	private P2Model p2model = new P2Model();
	/** Modifies the p2director args before it is run. */
	private Action<P2Model.MirrorApp> modifyAnt = Actions.doNothing();
	/** Run repo2runnable. */
	private boolean repo2runnable = false;

	/**
	 * Appropriate for PDE build - creates a subfolder `p2runnable` with all jars in their runnable form.
	 *
	 * Required especially for native launchers and target platforms (equinox.executable).
	 */
	public void repo2runnable() {
		repo2runnable = true;
	}
}
