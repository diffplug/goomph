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
package com.diffplug.gradle.pde;

import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.Task;

import com.google.common.collect.Sets;

import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;

/**
 * Runs P2 director to install artifacts from P2 repositories.
 * 
 * WARNING: This part of Goomph currently has the following precondition:
 * your project must have the property VER_ECLIPSE=4.5.2 (or some other version),
 * and you must have installed that Eclipse using Wuff. We will remove this
 * restriction in the future.
 * 
 * ```groovy
 * import com.diffplug.gradle.*
 * import com.diffplug.gradle.pde.*
 * import com.diffplug.common.swt.os.*

 * // list of OS values for which we want to create an installer
 * def INSTALLERS = OS.values()
 * def VER_JRE = '1.8.0.40'

 * // add each of the core IUs
 * def coreModel = new P2DirectorModel()
 * coreModel.addIU(APP_ID)
 * coreModel.addIU('com.diffplug.jre.feature.group')
 * coreModel.addIU('com.diffplug.native.feature.group')

 * // add each of the local repositories
 * def repoRoot = 'file:' + projectDir + '/'   // reads repos from this machine
 * //def repoRoot = 'http://192.168.1.77/'     // reads repos from another machine running hostFiles()
 * def assembleModel = coreModel.copy()
 * assembleModel.addRepo(repoRoot + P2_REPO_DIR)
 * ROOT_FEATURES.forEach() { feature ->
 *     assembleModel.addRepo('file:' + project.file(ROOT_FEATURE_DIR + feature))
 * }

 * // assemble DiffPlug for each os
 * task assembleAll
 * def ASSEMBLE_TASK = 'assemble'
 * def assembleDir(OS os) { return project.file('build/10_assemble' + os + (os.isMac() ? ".app" : "")) }
 * INSTALLERS.each() { os ->
 *     def assembleOneTask = assembleModel.taskFor(project, ASSEMBLE_TASK + os, os, assembleDir(os))
 *     assembleOneTask.dependsOn(diffplugP2)
 *     assembleOneTask.dependsOn(checkRootFeatures)
 *     assembleAll.dependsOn(assembleOneTask)

 *     // make the JRE executable if we can
 *     if (os.isMacOrLinux() && NATIVE_OS.isMacOrLinux()) {
 *         EclipsePlatform platform = EclipsePlatform.fromOS(os)
 *         assembleOneTask.doLast {
 *             def JRE_DIR = project.file(assembleDir(os).absolutePath + '/features/com.diffplug.jre.' + platform + '_' + VER_JRE + '/jre')
 *             CmdLine.runCmd(JRE_DIR, 'chmod -R a+x .')
 *         }
 *     }
 * }

 * // test case which creates a DiffPlug from an existing DiffPlug
 * task reassembleAll
 * def REASSEMBLE_TASK = 'reassemble'
 * def reassembleDir(OS os) { return project.file('build/11_reassemble' + os + (os.isMac() ? ".app" : "")) }
 * INSTALLERS.each() { os ->
 *     def reassembleModel = coreModel.copy()
 *     def reassembleRoot = 'file:' + assembleDir(os)
 *     reassembleModel.addMetadataRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.engine/profileRegistry/ProfileDiffPlugP2.profile')
 *     reassembleModel.addArtifactRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.core/cache')
 *     reassembleModel.addArtifactRepo(reassembleRoot)
 *     def reassembleOneTask = reassembleModel.taskFor(project, REASSEMBLE_TASK + os, os, reassembleDir(os))
 *     reassembleOneTask.dependsOn(ASSEMBLE_TASK + os)
 *     reassembleAll.dependsOn(reassembleOneTask)
 * }
 * ```
 */
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
