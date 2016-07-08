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

import com.diffplug.gradle.pde.EclipseRelease;

/** A declarative-style wrapper around a {@link P2Model}, appropriate for use as a DSL mixin. */
public interface P2Declarative {
	/** The underlying model. */
	P2Model getP2();

	default void repo(String repo) {
		getP2().addRepo(repo);
	}

	default void repoEclipse(String repo) {
		getP2().addRepoEclipse(repo);
	}

	default void repoEclipseLatest() {
		getP2().addRepoEclipse(EclipseRelease.LATEST);
	}

	default void metadataRepo(String repo) {
		getP2().addMetadataRepo(repo);
	}

	default void artifactRepo(String repo) {
		getP2().addArtifactRepo(repo);
	}

	default void iu(String iu) {
		getP2().addIU(iu);
	}

	default void iu(String iu, String version) {
		getP2().addIU(iu, version);
	}

	default void feature(String feature) {
		getP2().addFeature(feature);
	}

	default void feature(String feature, String version) {
		getP2().addFeature(feature, version);
	}
}
