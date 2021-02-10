/*
 * Copyright (C) 2016-2021 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.p2;


import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.pde.EclipseRelease;
import java.io.File;

/**
 * Creates a new folder for uploading to `repo.diffplug.com`, used by `4.5.2` and `4.7.2`.
 * 
 * However, for `4.13.0`, we took the following steps:
 * 
 * - run `eclipsec.exe -application org.eclipse.equinox.p2.director -repository https://download.eclipse.org/eclipse/updates/4.13/R-4.13-201909161045/ -installIU org.eclipse.equinox.p2.core.feature.feature.group,org.eclipse.equinox.p2.director.app,org.eclipse.equinox.p2.repository.tools,org.eclipse.core.net,org.eclipse.osgi.compatibility.state,org.eclipse.ant.core,org.apache.ant,org.eclipse.core.runtime,org.eclipse.update.configurator,org.eclipse.equinox.ds,org.eclipse.equinox.p2.reconciler.dropins -tag InitialState -destination result -profile SDKProfile -profileProperties org.eclipse.update.install.features=true`
 * - delete `features` and `p2/org.eclipse.equinox.p2.engine/.settings`
 * 
 */
public class P2BootstrapInstallationCreate {
	/** The release to install.  Make sure that build.gradle's org.eclipse.platform:org.eclipse.osgi:VERSION matches the version from this release. */
	static final EclipseRelease RELEASE = EclipseRelease.official("4.7.2");
	/** The place to install the release to. */
	static final File INSTALL_TO = new File(StandardSystemProperty.USER_HOME.value() + "/Desktop/bootstrap");

	public static void main(String[] args) throws Exception {
		FileMisc.cleanDir(INSTALL_TO);
		P2BootstrapInstallation installation = new P2BootstrapInstallation(RELEASE);
		P2Model model = installation.p2model();
		P2Model.DirectorApp app = model.directorApp(INSTALL_TO, "goomph-p2-bootstrap-" + RELEASE.version());
		app.roaming();
		app.runUsingBootstrapper();
	}
}
