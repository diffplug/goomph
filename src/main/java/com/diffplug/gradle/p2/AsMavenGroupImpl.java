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

import org.gradle.api.Project;

import com.diffplug.gradle.FileMisc;

/** Implementation of the p2 -> maven conversion. */
class AsMavenGroupImpl {
	final Project project;
	final File p2asmaven;
	final AsMavenGroup def;

	public AsMavenGroupImpl(Project project, File p2asmaven, AsMavenGroup group) {
		this.project = Objects.requireNonNull(project);
		this.p2asmaven = Objects.requireNonNull(p2asmaven);
		this.def = Objects.requireNonNull(group);
	}

	// @formatter:off
	File dirP2() {			return new File(p2asmaven, SUBDIR_P2 + "/" + def.group);			}
	File dirP2Runnable() {	return new File(p2asmaven, SUBDIR_P2_RUNNABLE + "/" + def.group);	}
	File dirMavenRoot() {	return new File(p2asmaven, SUBDIR_MAVEN);							}
	File dirMavenGroup() {	return new File(dirMavenRoot(), def.group);							}
	File tokenFile() {		return new File(p2asmaven, "token-" + def.group);					}
	// @formatter:on

	static final String SUBDIR_P2 = "p2";
	static final String SUBDIR_P2_RUNNABLE = "p2runnable";
	static final String SUBDIR_MAVEN = "maven";

	private P2AntRunner getApp() {
		P2Model cached = new P2Model();
		cached.addArtifactRepoBundlePool();
		cached.copyFrom(def.model);
		P2AntRunner app = cached.mirrorApp(dirP2());
		def.antModifier.execute(app);
		return app;
	}

	public void run() throws Exception {
		Objects.requireNonNull(def.group, "Must set mavengroup");
		// record the user's inputs
		String state = state();

		// if we've already written a token which confirms we're done with these inputs, then bail
		if (FileMisc.hasTokenFile(tokenFile(), state)) {
			project.getLogger().debug("p2AsMaven " + def.group + " is satisfied");
			return;
		} else {
			project.getLogger().lifecycle("p2AsMaven " + def.group + " is dirty.");
		}
		// else, we'll need to run our own little thing
		FileMisc.cleanDir(dirP2());
		FileMisc.cleanDir(dirP2Runnable());
		FileMisc.cleanDir(dirMavenGroup());

		// install from p2
		project.getLogger().lifecycle("Initalizing maven group " + def.group + " from p2");
		project.getLogger().lifecycle("Only needs to be done once, future builds will be much faster");

		project.getLogger().lifecycle("p2AsMaven " + def.group + " installing from p2");
		getApp().runUsingBootstrapper(project);

		if (def.repo2runnable) {
			project.getLogger().lifecycle("p2AsMaven " + def.group + " creating runnable repo");
			Repo2Runnable app = new Repo2Runnable();
			app.source(dirP2());
			app.destination(dirP2Runnable());
			app.runUsingBootstrapper(project);
		}

		// put p2 into a maven repo
		project.getLogger().lifecycle("p2AsMaven " + def.group + " creating maven repo");
		try (MavenRepoBuilder maven = new MavenRepoBuilder(dirMavenRoot())) {
			for (File plugin : FileMisc.list(new File(dirP2(), "plugins"))) {
				if (plugin.isFile() && plugin.getName().endsWith(".jar")) {
					maven.install(def.group, plugin);
				}
			}
		}

		// write out the staleness token to indicate that everything is good
		FileMisc.writeTokenFile(tokenFile(), state);
		project.getLogger().lifecycle("p2AsMaven " + def.group + " is complete.");
	}

	/** The args passed to p2 director represent the full state. */
	private String state() {
		return "mirrorApp: " + getApp().completeState() + "\nmavenGroup: " + def.group + "\ngoomph:" + GOOMPH_VERSION + "\nrepo2runnable:" + def.repo2runnable;
	}

	/** Bump this if we need to force people's deps to reload. */
	static final int GOOMPH_VERSION = 1;
}
