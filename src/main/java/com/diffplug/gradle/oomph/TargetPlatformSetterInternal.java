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
package com.diffplug.gradle.oomph;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.diffplug.gradle.osgi.OsgiExecable;

/** @see TargetPlatformSetter */
class TargetPlatformSetterInternal extends OsgiExecable.ReflectionClient<TargetPlatformSetter> {
	TargetPlatformSetterInternal(TargetPlatformSetter host) {
		super(host);
	}

	@Override
	public void run() {
		BundleContext bundleContext = FrameworkUtil.getBundle(OsgiExecable.class).getBundleContext();
		ServiceReference<ITargetPlatformService> reference = bundleContext.getServiceReference(ITargetPlatformService.class);
		ITargetPlatformService service = bundleContext.getService(reference);
		try {
			// create the target
			ITargetDefinition target = service.newTarget();
			ITargetLocation[] locations = new ITargetLocation[host.targetPlatforms.size()];
			for (int i = 0; i < locations.length; ++i) {
				String configuration = null; // default config location
				locations[i] = service.newProfileLocation(host.targetPlatforms.get(i).getAbsolutePath(), configuration);
			}
			target.setTargetLocations(locations);
			target.setName(host.name);
			service.saveTargetDefinition(target);
			// set it to be active
			LoadTargetDefinitionJob.load(target);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			bundleContext.ungetService(reference);
		}
	}
}
