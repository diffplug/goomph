/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.pde;


import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.p2.P2Model;
import java.io.File;

/** Creates a new folder for goomph-pde-bootstrap. */
public class PdeBootstrapInstallationCreate {
	/** The release to install. */
	static final EclipseRelease RELEASE = EclipseRelease.official("4.6.3");
	/** The place to install the release to. */
	static final File INSTALL_TO = new File(StandardSystemProperty.USER_HOME.value() + "/Desktop/bootstrap");

	public static void main(String[] args) throws Exception {
		FileMisc.cleanDir(INSTALL_TO);
		PdeInstallation installation = new PdeInstallation(RELEASE);
		P2Model model = installation.p2model();
		model.addMetadataRepo(INSTALL_TO);
		model.addArtifactRepo(INSTALL_TO);
		P2Model.DirectorApp app = model.directorApp(INSTALL_TO, "goomph-pde-bootstrap-" + RELEASE.version());
		app.roaming();
		app.runUsingBootstrapper();
	}
}
