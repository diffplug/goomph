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
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.gradle.api.Project;
import org.osgi.framework.Version;

import com.diffplug.common.base.Comparison;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.CmdLine;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.eclipse.EclipseArgsBuilder;
import com.diffplug.gradle.eclipse.EclipseRelease;
import com.diffplug.gradle.p2.P2DirectorModel;

/** Wraps a PDE installation for the given eclipse release. */
class PdeInstallation {
	/** Returns a PdeInstallation appropriate for this project. */
	static PdeInstallation fromProject(Project project) {
		String version = (String) project.getProperties().get("GOOMPH_PDE_VER");
		String updateSite = (String) project.getProperties().get("GOOMPH_PDE_UDPATE_SITE");
		String id = (String) project.getProperties().get("GOOMPH_PDE_ID");

		// to use a default PDE build, use
		String USAGE = StringPrinter.buildStringFromLines(
				"You must specify which version of Eclipse should be used by Goomph.",
				"Option #1: To use an officially supported release, use this:",
				"GOOMPH_PDE_VER=4.5.2 (or any of " + EclipseRelease.supportedRange() + ")",
				"Option #2: To use any release (e.g. milestone, nightly, etc)",
				"GOOMPH_PDE_VER=<any version>",
				"GOOMPH_PDE_UDPATE_SITE=<url to update site>",
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
		return new PdeInstallation(EclipseRelease.createWithIdVersionUpdatesite(id, version, updateSite));
	}

	final EclipseRelease release;

	public PdeInstallation(EclipseRelease release) {
		this.release = Objects.requireNonNull(release);
	}

	/** The root of this installation. */
	File getRootFolder() {
		return new File(GoomphCacheLocations.pdeBootstrap(), release.toString());
	}

	static final String TOKEN = "installed";

	/** Makes sure that the installation is prepared. */
	void ensureInstalled() throws Exception {
		if (!isInstalled()) {
			install();
		}
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
	public EclipseArgsBuilder productBuildCmd(File buildDir) throws Exception {
		EclipseArgsBuilder args = antBuildCmd(getPdeBuildProductBuildXml());
		args.addArg("Dbuilder=" + quote(buildDir));
		return args;
	}

	/** Returns a command which will execute the PDE builder for a generic ant build file. */
	public EclipseArgsBuilder antBuildCmd(File buildfile) {
		EclipseArgsBuilder args = new EclipseArgsBuilder();
		args.application("org.eclipse.ant.core.antRunner");
		args.addArg("buildfile", quote(buildfile));
		return args;
	}

	/**
	 * The "org.eclipse.pde.build" folder for this installation.
	 * 
	 * Set when install() succeeds and when isInstalled() returns true, so it is
	 * guaranteed to be set when ensureInstalled completes.
	 */
	private File pdeBuildFolder;

	/** Returns true iff it is installed. */
	private boolean isInstalled() throws IOException {
		Optional<String> pdeBuild = FileMisc.readToken(getRootFolder(), TOKEN);
		pdeBuildFolder = pdeBuild.map(File::new).orElse(null);
		return pdeBuildFolder != null;
	}

	/** Installs the bootstrap installation. */
	private void install() throws Exception {
		System.out.print("Installing pde " + release + "... ");
		p2model().install(getRootFolder(), "goomph-pde-bootstrap-" + release, config -> {
			// share the install for quickness
			config.bundlepool(GoomphCacheLocations.bundlePool());
			// create a native launcher
			config.oswsarch(SwtPlatform.getRunning());
		});
		File sharedPlugins = new File(GoomphCacheLocations.bundlePool(), "plugins");
		File[] pdeBuilds = sharedPlugins.listFiles(file -> {
			return file.isDirectory() && file.getName().startsWith("org.eclipse.pde.build_");
		});
		pdeBuildFolder = pdeBuilds[0];
		FileMisc.writeToken(getRootFolder(), TOKEN, pdeBuildFolder.getAbsolutePath());
		System.out.println("Success.");
	}

	/**
	 * Creates a model containing pde build and the native launder.
	 */
	P2DirectorModel p2model() {
		P2DirectorModel model = new P2DirectorModel();
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

	/** Returns true iff this Eclipse is Mars or later. */
	private boolean isMarsOrLater() {
		return Comparison.compare(release.version(), MARS).lesserEqualGreater(false, true, true);
	}

	private static final Version MARS = Version.parseVersion("4.5.0");

	/** Returns the eclipse console executable. */
	private File getEclipseConsoleExecutable() {
		OS os = OS.getNative();
		if (os.isWindows()) {
			return new File(getRootFolder(), "eclipsec.exe");
		} else if (os.isMac()) {
			String path = isMarsOrLater() ? "../MacOS/eclipse" : "eclipse.app/Contents/MacOS/eclipse";
			return new File(path);
		} else if (os.isLinux()) {
			return new File("eclipse");
		} else {
			throw Unhandled.objectException(os);
		}
	}

	/** Runs the given arguments using the eclipsec embedded in this PdeInstallation. */
	public void run(EclipseArgsBuilder args) throws Exception {
		ensureInstalled();

		StringBuilder builder = new StringBuilder();
		// add eclipsec
		builder.append(quote(getEclipseConsoleExecutable().getCanonicalFile()));
		for (String arg : args.toArgList()) {
			// space
			builder.append(' ');
			// arg (possibly quoted)
			if (arg.contains(" ")) {
				builder.append('"');
				builder.append(arg);
				builder.append('"');
			} else {
				builder.append(arg);
			}
		}
		// execute the cmd
		CmdLine.runCmd(builder.toString());
	}

	/** Returns the absolute path quoted. */
	private static String quote(File file) {
		return "\"" + file.getAbsolutePath() + "\"";
	}
}
