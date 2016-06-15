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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import groovy.lang.Closure;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.collect.Sets;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GroovyCompat;
import com.diffplug.gradle.eclipse.EclipseArgsBuilder;
import com.diffplug.gradle.eclipse.EquinoxLauncher;

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
 *
 * // list of OS values for which we want to create an installer
 * def INSTALLERS = OS.values()
 * def VER_JRE = '1.8.0.40'
 *
 * // add each of the core IUs
 * def coreModel = new P2DirectorModel()
 * coreModel.addIU(APP_ID)
 * coreModel.addIU('com.diffplug.jre.feature.group')
 * coreModel.addIU('com.diffplug.native.feature.group')
 *
 * // add each of the local repositories
 * def repoRoot = 'file:' + projectDir + '/'   // reads repos from this machine
 * //def repoRoot = 'http://192.168.1.77/'     // reads repos from another machine running hostFiles()
 * def assembleModel = coreModel.copy()
 * assembleModel.addRepo(repoRoot + P2_REPO_DIR)
 * ROOT_FEATURES.forEach() { feature ->
 *     assembleModel.addRepo('file:' + project.file(ROOT_FEATURE_DIR + feature))
 * }
 *
 * // assemble DiffPlug for each os
 * task assembleAll
 * def ASSEMBLE_TASK = 'assemble'
 * def assembleDir(OS os) { return project.file('build/10_assemble' + os + (os.isMac() ? ".app" : "")) }
 * INSTALLERS.each() { os ->
 *     def assembleOneTask = assembleModel.taskFor(project, ASSEMBLE_TASK + os, os, assembleDir(os))
 *     assembleOneTask.dependsOn(diffplugP2)
 *     assembleOneTask.dependsOn(checkRootFeatures)
 *     assembleAll.dependsOn(assembleOneTask)
 *
 *     // make the JRE executable if we can
 *     if (os.isMacOrLinux() && NATIVE_OS.isMacOrLinux()) {
 *         EclipsePlatform platform = EclipsePlatform.fromOS(os)
 *         assembleOneTask.doLast {
 *             def JRE_DIR = project.file(assembleDir(os).absolutePath + '/features/com.diffplug.jre.' + platform + '_' + VER_JRE + '/jre')
 *             CmdLine.runCmd(JRE_DIR, 'chmod -R a+x .')
 *         }
 *     }
 * }
 *
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

	/** Combines all fields for easy implementation of equals and hashCode. */
	private final List<Object> content = Arrays.asList(ius, repos, metadataRepo, artifactRepo);

	/** Hash of the models current content. */
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	/** Two models are equal if all their fields are equal. */
	@Override
	public boolean equals(Object otherObj) {
		if (otherObj instanceof P2DirectorModel) {
			return content.equals(((P2DirectorModel) otherObj).content);
		} else {
			return false;
		}
	}

	public void addIU(String iu) {
		ius.add(iu);
	}

	public void addIU(String iu, String version) {
		ius.add(iu + "/" + version);
	}

	public void addFeature(String feature) {
		addIU(feature + ".feature.group");
	}

	public void addFeature(String feature, String version) {
		addIU(feature + ".feature.group", version);
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

	/**
	 * Returns the arguments required to call "eclipsec" and run the p2 director application
	 * to install the artifacts from the repos in this model into the given directory and profile.
	 */
	public ArgsBuilder argsForInstall(File dstFolder, String profile) {
		ArgsBuilder builder = new ArgsBuilder();
		builder.clean();
		builder.consolelog();
		builder.application("org.eclipse.equinox.p2.director");
		repos.forEach(repo -> builder.addArg("repository", repo));
		metadataRepo.forEach(repo -> builder.addArg("metadataRepository", repo));
		artifactRepo.forEach(repo -> builder.addArg("artifactRepository", repo));
		ius.forEach(iu -> builder.addArg("installIU", iu));
		builder.addArg("profile", profile);
		builder.addArg("destination", "file:" + dstFolder.getAbsolutePath());
		return builder;
	}

	/**
	 * An extension of EclipseArgsBuilder with typed methods appropriate for p2 director.
	 *
	 * Created using {@link P2DirectorModel#argsForInstall(File, String)}.
	 */
	public static class ArgsBuilder extends EclipseArgsBuilder {
		/**
		 * Adds a `bundlepool` argument.
		 *
		 * The location of where the plug-ins and features will be stored. This value
		 * is only taken into account when a new profile is created. For an application
		 * where all the bundles are located into the plugins/ folder of the destination,
		 * set it to `<destination>`.
		 */
		public void bundlepool(File bundlePool) {
			addArg("bundlepool", bundlePool.getAbsolutePath());
		}

		/** Adds `p2.os`, `p2.ws`, and `p2.arch` arguments. */
		public void oswsarch(SwtPlatform platform) {
			addArg("p2.os", platform.getOs());
			addArg("p2.ws", platform.getWs());
			addArg("p2.arch", platform.getArch());
		}

		/**
		 * Adds the `roaming` argument.
		 *
		 * Indicates that the product resulting from the installation can be moved.
		 * This property only makes sense when the destination and bundle pool are
		 * in the same location. This value is only taken into account when the
		 * profile is created.
		 */
		public void roaming() {
			addArg("roaming");
		}

		/**
		 * Adds the `shared` argument.
		 *
		 * use a shared location for the install. The path defaults to ${user.home}/.p2.
		 */
		public void shared() {
			addArg("shared");
		}

		/** @see #shared() */
		public void shared(File shared) {
			addArg("shared", shared.getAbsolutePath());
		}
	}

	/**
	 * Installs the IUs from the repos specified in this object into the given folder.
	 *
	 * This uses the P2 director application from the latest available 
	 *
	 * For more args, see [the p2 director docs](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html&cp=2_0_20_2).
	 *
	 * In particular, you might want to add "roaming", if you'd like to copy-paste an installation around.
	 *
	 * @param dstFolder the folder into which the installation will take place.
	 * @param profile the name of the profile, doesn't really matter what it is.
	 */
	public void install(File dstFolder, String profile, Consumer<ArgsBuilder> configModify) throws Exception {
		// setup the args
		ArgsBuilder args = argsForInstall(dstFolder, profile);
		configModify.accept(args);
		// ensure the bootstrap installation is installed
		P2BootstrapInstallation installation = P2BootstrapInstallation.latest();
		installation.ensureInstalled();
		// launch the equinox application with these arguments
		EquinoxLauncher launcher = new EquinoxLauncher(installation.getRootFolder());
		launcher.setArgs(args.toArgList());
		launcher.run();
	}

	/** Groovy-friendly version of {@link P2DirectorModel#install(File, String, Consumer)}. */
	public void install(File dstFolder, String profile, Closure<ArgsBuilder> configModify) throws Exception {
		install(dstFolder, profile, GroovyCompat.consumerFrom(configModify));
	}

	/** See {@link #install(File, String, Consumer)}. */
	public void install(File dstFolder, String profile) throws Exception {
		install(dstFolder, profile, Consumers.doNothing());
	}

	/** Deletes the cached repository info (which may include references to local paths). */
	public static void cleanCachedRepositories(File dstFile) throws IOException {
		Path path = dstFile.toPath().resolve("p2/org.eclipse.equinox.p2.engine/.settings");
		FileMisc.cleanDir(path.toFile());
	}
}
