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

import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.pde.EclipseRelease;

public class P2Test {
	public static void main(String[] unused) throws Exception {
		File dir = new File("C:\\Users\\ntwigg\\Documents\\DiffPlugDev\\DiffPlug\\targetplatform\\build\\p2asmaven\\__p2__");
		FileMisc.cleanDir(dir);

		P2Model model = new P2Model();
		model.addRepo(EclipseRelease.official("4.5.2").updateSite());
		model.addArtifactRepo(GoomphCacheLocations.bundlePool());
		model.addFeature("org.eclipse.equinox.executable");
		model.addFeature("org.eclipse.rcp.configuration");
		model.mirrorApp(dir).runUsingBootstrapper();
	}
}
