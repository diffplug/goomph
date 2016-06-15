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
import java.util.function.Consumer;

import org.gradle.api.Project;

import groovy.lang.Closure;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.GroovyCompat;

public class P2Mavenify {
	public static void init(Project project, Closure<P2Mavenify> configure) {
		init(project, GroovyCompat.consumerFrom(configure));
	}

	private static void init(Project project, Consumer<P2Mavenify> configure) {
		project.afterEvaluate(p -> {
			P2Mavenify mavenify = new P2Mavenify(project);
			configure.accept(mavenify);
			Errors.rethrow().run(mavenify::run);
		});
	}

	final Project project;

	private P2Mavenify(Project project) {
		this.project = project;
	}

	private void run() throws Exception {
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
		project.getLogger().info("Initalizing maven group " + mavenGroup + " from p2");
		project.getLogger().info("Only needs to be done once, future builds will be much faster");
		if (useBundlePool) {
			project.getLogger().debug("P2Mavenify " + mavenGroup + " installing to bundle pool cache");
			p2model.install(dir, mavenGroup, config -> {
				config.bundlepool(GoomphCacheLocations.bundlePool());
			});
			project.getLogger().debug("P2Mavenify " + mavenGroup + " installing from bundle pool cache");
			FileMisc.cleanDir(dir);
			p2model.addArtifactRepo(GoomphCacheLocations.bundlePool());
			p2model.install(dir, mavenGroup);
		} else {
			project.getLogger().debug("P2Mavenify " + mavenGroup + " installing from p2");
			p2model.install(dir, mavenGroup);
		}
		project.getLogger().debug("P2Mavenify " + mavenGroup + " completed successfully");

		// write out the staleness token to indicate that everything is good
		FileMisc.writeToken(dir, STALE_TOKEN, state);
	}

	static final String STALE_TOKEN = "stale_token";

	private String state() {
		return mavenGroup + useBundlePool + destination + p2model.hashCode();
	}

	public void mavenGroup(String mavenGroup) {
		this.mavenGroup = mavenGroup;
	}

	public void destination(String mavenGroup) {
		this.mavenGroup = mavenGroup;
	}

	public void useBundlePool(String mavenGroup) {
		this.mavenGroup = mavenGroup;
	}

	public void p2(Closure<P2DirectorModel> modelConfig) {
		GroovyCompat.consumerFrom(modelConfig).accept(p2model);
	}

	/** The group which will be used in the maven-ization. */
	private String mavenGroup;
	/** When this is true, the global bundle pool will be used to accelerate artifact downloads. */
	private Object destination = "build/goomph-m2";
	/** When this is true, the global bundle pool will be used to cache and accelerate artifact downloads. */
	private boolean useBundlePool = true;
	/** The model we'd like to download. */
	private P2DirectorModel p2model = new P2DirectorModel();
}
