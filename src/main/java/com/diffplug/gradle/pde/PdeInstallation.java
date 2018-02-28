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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.osgi.framework.Version;

import com.diffplug.common.base.Comparison;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ZipMisc;
import com.diffplug.gradle.eclipserunner.EclipseApp;
import com.diffplug.gradle.eclipserunner.EclipseRunner;
import com.diffplug.gradle.eclipserunner.NativeRunner;
import com.diffplug.gradle.p2.P2Model;

/** Wraps a PDE installation for the given eclipse release.*/
public class PdeInstallation implements EclipseRunner {

	static final String DOWNLOAD_FILE = "/goomph-pde-bootstrap.zip";
	static final String VERSIONED_DOWNLOAD_FILE = "/goomph-pde-bootstrap-%s.zip";

	/**
	 * Returns a PdeInstallation based on `GOOMPH_PDE_VER`, and other factors.
	 *
	 * You must specify which version of Eclipse should be used by Goomph.
	 * - Option #1: To use an officially supported release, use this:
	 *     + `GOOMPH_PDE_VER`=4.5.2 (or any official release)
	 * - Option #2: To use any release (e.g. milestone, nightly, etc)
	 *     + `GOOMPH_PDE_VER`=<any version>
	 *     + `GOOMPH_PDE_UPDATE_SITE`=<url to update site>
	 *     + `GOOMPH_PDE_ID`=<the ID used for caching, cannot be a version listed in Option #1)
	 *
	 * You must do one or the other, specify only `VER` for Option #1,
	 * or specify `VER`, `UPDATE_SITE`, and `ID` for Option #2.
	 */
	public static PdeInstallation fromProject(Project project) {
		String version = (String) project.getProperties().get("GOOMPH_PDE_VER");

		String deprecatedUpdateSite = (String) project.getProperties().get("GOOMPH_PDE_UDPATE_SITE");
		if (deprecatedUpdateSite != null) {
			project.getLogger().warn("Property GOOMPH_PDE_UDPATE_SITE is deprecated, please use GOOMPH_PDE_UPDATE_SITE instead.");
		}

		String updateSite = Optional
				.ofNullable((String) project.getProperties().get("GOOMPH_PDE_UPDATE_SITE"))
				.orElse(deprecatedUpdateSite);

		String id = (String) project.getProperties().get("GOOMPH_PDE_ID");

		// to use a default PDE build, use
		String USAGE = StringPrinter.buildStringFromLines(
				"You must specify which version of Eclipse should be used by Goomph.",
				"Option #1: To use an officially supported release, use this:",
				"GOOMPH_PDE_VER=4.5.2 (or any of " + EclipseRelease.supportedRange() + ")",
				"Option #2: To use any release (e.g. milestone, nightly, etc)",
				"GOOMPH_PDE_VER=<any version>",
				"GOOMPH_PDE_UPDATE_SITE=<url to update site>",
				"GOOMPH_PDE_ID=<the ID used for caching, cannot be a version listed in Option #1)",
				"",
				"You must do one or the other, specify only VER for Option #1,",
				"or specify VER, UPDATE_SITE, and ID for Option #2");
		if (version == null) {
			throw new IllegalArgumentException(USAGE);
		}
		if (updateSite == null && id == null) {
			try {
				return new PdeInstallation(EclipseRelease.official(version));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(USAGE, e);
			}
		}
		if (updateSite == null || id == null) {
			throw new IllegalArgumentException(USAGE);
		}
		return from(EclipseRelease.createWithIdVersionUpdatesite(id, version, updateSite));
	}

	/** Returns an EclipseRunner for running PDE build against the given release. */
	public static PdeInstallation from(EclipseRelease release) {
		return new PdeInstallation(release);
	}

	final EclipseRelease release;

	private Function<PdeInstallation, File> workspaceProvider = PdeInstallation::getDefaultWorkspace;

	public PdeInstallation(EclipseRelease release) {
		this.release = Objects.requireNonNull(release);
		// warn if mac and pre-Mars
		if (OS.getNative().isMac()) {
			if (Comparison.compare(release.version(), Version.parseVersion("4.5.0")) == Comparison.LESSER) {
				throw new IllegalArgumentException("On mac, must be 4.5.0 (Mars) or later, because of folder layout problems.");
			}
		}
	}

	/** Allows to set a different workspace provider for custom workspace usage. */
	public void setWorkspaceProvider(Function<PdeInstallation, File> workspaceProvider) {
		this.workspaceProvider = workspaceProvider;
	}

	/** The root of this installation. */
	private File getRootFolder() {
		return new File(GoomphCacheLocations.pdeBootstrap(), release.toString() + FileMisc.macApp());
	}

	/** The `org.eclipse.pde.build` folder containing the product build properties file. */
	File getPdeBuildFolder() throws Exception {
		ensureInstalled();
		return Objects.requireNonNull(pdeBuildFolder);
	}

	/** Returns the "productBuild.xml" file. */
	File getPdeBuildProductBuildXml() throws Exception {
		return getPdeBuildFolder().toPath()
				.resolve("scripts/productBuild/productBuild.xml")
				.toFile();
	}

	/** Returns a command which will execute the PDE builder for a product. */
	public EclipseApp productBuildCmd(File buildDir) throws Exception {
		EclipseApp antApp = new EclipseApp(EclipseApp.AntRunner.ID);
		antApp.addArg("buildfile", getPdeBuildProductBuildXml().getAbsolutePath());
		antApp.addArg("Dbuilder=" + FileMisc.quote(buildDir));
		return antApp;
	}

	/**
	 * The "org.eclipse.pde.build" folder for this installation.
	 *
	 * Set when install() succeeds and when isInstalled() returns true, so it is
	 * guaranteed to be set when ensureInstalled completes.
	 */
	private File pdeBuildFolder;

	static final String TOKEN = "installed";

	/** Makes sure that the installation is prepared. */
	private void ensureInstalled() throws Exception {
		if (!isInstalled()) {
			install();
		}
	}

	/** Returns true iff it is installed. */
	private boolean isInstalled() throws IOException {
		Optional<String> pdeBuild = FileMisc.readToken(getRootFolder(), TOKEN);
		pdeBuildFolder = pdeBuild.map(File::new).orElse(null);
		return pdeBuildFolder != null;
	}

	/** Installs the bootstrap installation. */
	private void install() throws Exception {

		if (GoomphCacheLocations.pdeBootstrapUrl().isPresent()) {
			String url = GoomphCacheLocations.pdeBootstrapUrl().get();
			System.out.print("Installing pde " + release + " from " + url + "... ");
			File target = new File(getRootFolder(), DOWNLOAD_FILE);
			try {
				obtainBootstrap(url + release.version() + DOWNLOAD_FILE, target);
			} catch (FileNotFoundException ex) {
				//try versioned artifact - Common when bootstrap is on a maven type(sonatype nexus, etc.) repository.
				obtainBootstrap(url + release.version() + String.format(VERSIONED_DOWNLOAD_FILE, release.version()), target);
			}
			// unzip it
			ZipMisc.unzip(target, target.getParentFile());
			// delete the zip
			FileMisc.forceDelete(target);
		} else {
			System.out.print("Installing pde " + release + "... ");
			obtainBootstrap(release);
		}

		// parse out the pde.build version
		File bundleInfo = new File(getContentsEclipse(), "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		Preconditions.checkArgument(bundleInfo.isFile(), "Needed to find the pde.build folder: %s", bundleInfo);
		String pdeBuildLine = Files.readAllLines(bundleInfo.toPath()).stream().filter(line -> line.startsWith("org.eclipse.pde.build,")).findFirst().get();
		String pdeBuildVersion = pdeBuildLine.split(",")[1];
		// find the plugins folder
		pdeBuildFolder = new File(GoomphCacheLocations.bundlePool(), "plugins/org.eclipse.pde.build_" + pdeBuildVersion);
		FileMisc.writeToken(getRootFolder(), TOKEN, pdeBuildFolder.getAbsolutePath());
		System.out.println("Success.");
	}

	/** Obtain PDE Installation from custom file or url */
	private void obtainBootstrap(String bootstrapUrl, File target) throws IOException {
		URL url = new URL(bootstrapUrl);
		FileUtils.copyURLToFile(url, target);
	}

	/** Obtain PDE Installation from remote p2 repository */
	private void obtainBootstrap(EclipseRelease release) throws Exception {
		P2Model.DirectorApp directorApp = p2model().directorApp(getRootFolder(), "goomph-pde-bootstrap-" + release);
		// share the install for quickness
		directorApp.bundlepool(GoomphCacheLocations.bundlePool());
		// create a native launcher
		directorApp.platform(SwtPlatform.getRunning());
		directorApp.runUsingBootstrapper();
	}

	/** Returns the Contents/Eclipse folder on mac, or just the root folder on other OSes. */
	private File getContentsEclipse() {
		if (OS.getNative().isMac()) {
			return new File(getRootFolder(), "Contents/Eclipse");
		} else {
			return getRootFolder();
		}
	}

	/** Creates a model containing pde build and the native launder. */
	P2Model p2model() {
		P2Model model = new P2Model();
		// the update site for the release we're downloading artifacts for
		model.addRepo(release.updateSite());
		// the required IDE root product
		model.addIU("org.eclipse.platform.ide");
		// ant builder
		model.addFeature("org.eclipse.jdt");
		// pde build
		model.addFeature("org.eclipse.pde");
		return model;
	}

	/** Returns the eclipse console executable. */
	private String getEclipseConsoleExecutable() {
		return OS.getNative().winMacLinux(
				"eclipsec.exe",
				"Contents/MacOS/eclipse",
				"eclipse");
	}

	private static File getDefaultWorkspace(PdeInstallation installation) {
		// Use default...
		return new File(installation.getRootFolder(), FileMisc.macContentsEclipse() + "workspace");
	}

	@Override
	public void run(List<String> args) throws Exception {
		ensureInstalled();
		// set a clean workspace
		List<String> actualArgs = new ArrayList<>();
		actualArgs.add("-data");
		File workspace = workspaceProvider.apply(this);
		actualArgs.add(workspace.getAbsolutePath());
		// add the user's args
		actualArgs.addAll(args);
		// run the code
		try {
			new NativeRunner(new File(getRootFolder(), getEclipseConsoleExecutable())).run(actualArgs);
		} finally {
			// clean the workspace directory
			FileUtils.deleteDirectory(workspace);
		}
	}
}
