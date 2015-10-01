/*
 * Copyright 2015 DiffPlug
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
package com.diffplug.gradle.eclipse;

import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;

import com.google.common.collect.Sets;

import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;

public class P2DirectorModel {
	/** Returns a deep copy of this model. */
	public P2DirectorModel copy() {
		P2DirectorModel copy = new P2DirectorModel();
		copy.ius.addAll(ius);
		copy.repos.addAll(repos);
		copy.metadataRepo.addAll(metadataRepo);
		copy.artifactRepo.addAll(artifactRepo);
		return copy;
	}

	private Set<String> ius = Sets.newHashSet();
	private Set<String> repos = Sets.newLinkedHashSet();
	private Set<String> metadataRepo = Sets.newLinkedHashSet();
	private Set<String> artifactRepo = Sets.newLinkedHashSet();

	public void addIU(String iu) {
		ius.add(iu);
	}

	public void addRepo(String repo) {
		repos.add(repo);
	}

	public void addMetadataRepo(String repo) {
		metadataRepo.add(repo);
	}

	public void addArtifactRepo(String repo) {
		artifactRepo.add(repo);
	}

	private static final String PROFILE = "ProfileDiffPlugP2";

	/** Creates a task which run P2 Director on the given model. */
	public Task taskFor(Project project, String taskName, OS os, Object dstDir) throws Exception {
		// create an EclipseTask
		EclipseTask task = project.getTasks().create(taskName, EclipseTask.class);
		// set it up to build
		task.addArg("nosplash", "");
		task.addArg("application", "org.eclipse.equinox.p2.director");
		repos.forEach(repo -> task.addArg("repository", repo));
		metadataRepo.forEach(repo -> task.addArg("metadataRepository", repo));
		artifactRepo.forEach(repo -> task.addArg("artifactRepository", repo));
		ius.forEach(iu -> task.addArg("installIU", iu));
		task.addArg("profile", PROFILE);
		task.addArg("profileProperties", "org.eclipse.update.install.features=true");

		SwtPlatform platform = SwtPlatform.fromOS(os);
		task.addArg("p2.os", platform.getOs());
		task.addArg("p2.ws", platform.getWs());
		task.addArg("p2.arch", platform.getArch());

		task.addArg("roaming", "");

		task.addArg("destination", "file:" + project.file(dstDir).getAbsolutePath());

		return task;
	}
}
