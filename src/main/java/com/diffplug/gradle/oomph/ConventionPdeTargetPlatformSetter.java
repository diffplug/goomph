/*
 * Copyright (C) 2015-2019 DiffPlug
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
package com.diffplug.gradle.oomph;


import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;

class ConventionPdeTargetPlatformSetter extends SetupAction.Internal<ConventionPde.TargetPlatformSetter> {
	ConventionPdeTargetPlatformSetter(ConventionPde.TargetPlatformSetter host) {
		super(host);
	}

	@Override
	protected void runWithinEclipse() throws Throwable {
		EclipseMisc.withService(ITargetPlatformService.class, targetPlatformService -> {
			// create the target
			ITargetDefinition target = targetPlatformService.newTarget();
			ITargetLocation[] locations = new ITargetLocation[host.installations.size()];
			for (int i = 0; i < locations.length; ++i) {
				String configuration = null; // default config location
				locations[i] = targetPlatformService.newProfileLocation(host.installations.get(i).getAbsolutePath(), configuration);
			}
			target.setTargetLocations(locations);
			target.setName(host.name);
			targetPlatformService.saveTargetDefinition(target);
			// set it to be active
			LoadTargetDefinitionJob.load(target);
		});
		// wait for the target to load
		EclipseMisc.waitForJobsToFinish();
		// and save the workspace
		SaveWorkspaceInternal.save();
	}
}
