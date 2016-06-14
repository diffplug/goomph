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
import java.net.URL;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ZipUtil;
import com.diffplug.gradle.eclipse.EclipseRelease;

/** Wraps a Bootstrap installation for the given eclipse release. */
class P2BootstrapInstallation {
	static final String DOWNLOAD_ROOT = "https://dl.bintray.com/diffplug/opensource/com/diffplug/gradle/goomph-p2-bootstrap/";
	static final String DOWNLOAD_FILE = "/goomph-p2-bootstrap.zip";

	final EclipseRelease release;

	P2BootstrapInstallation(EclipseRelease release) {
		this.release = Objects.requireNonNull(release);
	}

	/** The root of this installation. */
	File getRootFolder() {
		return new File(GoomphCacheLocations.p2bootstrap(), release.version().toString());
	}

	/** Makes sure that the installation is prepared. */
	public void ensureInstalled() throws IOException {
		if (!isInstalled()) {
			install();
		}
	}

	static final String TOKEN = "installed";

	/** Returns true iff it is installed. */
	private boolean isInstalled() throws IOException {
		return FileMisc.hasToken(getRootFolder(), TOKEN);
	}

	/** Installs the bootstrap installation. */
	private void install() throws IOException {
		// clean the install folder
		FileMisc.cleanDir(getRootFolder());
		// download the URL
		File target = new File(getRootFolder(), DOWNLOAD_FILE);
		URL url = new URL(DOWNLOAD_ROOT + release.version() + DOWNLOAD_FILE);
		FileUtils.copyURLToFile(url, target);
		// unzip it
		ZipUtil.unzip(target, target.getParentFile());
		// delete the zip
		target.delete();
		FileMisc.writeToken(getRootFolder(), TOKEN);
	}

	/**
	 * Creates a model containing p2-director for the given {@link EclipseRelease}.
	 *
	 * Useful for updating [bintray](https://bintray.com/diffplug/opensource/goomph-p2-bootstrap/view).
	 */
	P2DirectorModel p2model() {
		P2DirectorModel model = new P2DirectorModel();
		// the update site for the release we're downloading artifacts for
		model.addRepo(release.updateSite());
		// the p2 director application and its dependencies
		model.addIU("org.eclipse.equinox.p2.director.app");
		model.addFeature("org.eclipse.equinox.p2.core.feature");
		// failed transitive required for basic p2 operations
		model.addIU("org.eclipse.core.net");
		// failed transitive required for shared installations touchpoints
		model.addIU("org.eclipse.osgi.compatibility.state");
		// eclipse infrastructure to make "eclipsec -application org.eclipse.equinox.p2.director" work
		model.addIU("org.eclipse.core.runtime");
		model.addIU("org.eclipse.update.configurator");
		model.addIU("org.eclipse.equinox.ds");
		return model;
	}
}
