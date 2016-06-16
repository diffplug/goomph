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
import java.util.Optional;

import org.gradle.api.Project;

import com.diffplug.gradle.FileMisc;

/** Implementation of the p2 -> maven conversion. */
class AsMaven {
	final Project project;

	public AsMaven(Project project) {
		this.project = project;
	}

	public File getDestination() {
		return project.file(destination);
	}

	public void run() throws Exception {
		Objects.requireNonNull(mavenGroup, "Must set mavengroup");
		// record the user's inputs
		String state = state();

		// if we've already written a token which confirms we're done with these inputs, then bail
		File dir = project.file(destination);
		Optional<String> result = FileMisc.readToken(dir, STALE_TOKEN);
		if (result.isPresent()) {
			if (result.get().equals(state)) {
				project.getLogger().debug("P2Mavenify " + mavenGroup + " is satisfied");
				return;
			} else {
				project.getLogger().info("P2Mavenify " + mavenGroup + " is dirty, redoing");
			}
		}
		// else, we'll need to run our own little thing
		FileMisc.cleanDir(dir);

		// install from p2
		project.getLogger().info("Initalizing maven group " + mavenGroup + " from p2");
		project.getLogger().info("Only needs to be done once, future builds will be much faster");

		File p2Dir = new File(dir, "__p2__");
		project.getLogger().debug("P2Mavenify " + mavenGroup + " installing from p2");
		p2model.install(p2Dir, mavenGroup);

		// put the p2 into a maven repo
		project.getLogger().debug("P2Mavenify " + mavenGroup + " creating maven repo");
		try (MavenRepoBuilder maven = new MavenRepoBuilder(dir)) {
			for (File plugin : new File(p2Dir, "plugins").listFiles()) {
				if (plugin.isFile() && plugin.getName().endsWith(".jar")) {
					maven.install(mavenGroup, plugin);
				}
			}
		}

		// write out the staleness token to indicate that everything is good
		FileMisc.writeToken(dir, STALE_TOKEN, state);
	}

	static final String STALE_TOKEN = "stale_token";

	private String state() {
		return mavenGroup + destination + p2model.hashCode() + GOOMPH_VERSION;
	}

	/** Bump this if we need to force people's deps to reload. */
	static final int GOOMPH_VERSION = 1;

	public void mavenGroup(String mavenGroup) {
		this.mavenGroup = mavenGroup;
	}

	public void destination(Object destination) {
		this.destination = destination;
	}

	public P2DirectorModel p2() {
		return p2model;
	}

	/** The group which will be used in the maven-ization. */
	private String mavenGroup;
	/** When this is true, the global bundle pool will be used to accelerate artifact downloads. */
	private Object destination = "build/goomph-asmaven";
	/** The model we'd like to download. */
	private P2DirectorModel p2model = new P2DirectorModel();
}
