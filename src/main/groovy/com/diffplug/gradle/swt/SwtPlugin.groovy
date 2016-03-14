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
package com.diffplug.gradle.swt

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

import com.diffplug.common.base.Unhandled
import com.diffplug.common.swt.os.SwtPlatform

public class SwtPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// ensure we don't double-apply the plugin
		if (project.getPlugins().hasPlugin(SwtPlugin.class)) {
			return
		}

		// make sure the java plugin has been applied
		if (!project.getPlugins().hasPlugin(JavaPlugin.class)) {
			project.getPlugins().apply(JavaPlugin.class)
		}

		// create the SwtExtension
		SwtExtension extension = project.getExtensions().create(SwtExtension.NAME, SwtExtension.class)

		project.afterEvaluate {
			project.repositories {
				ivy {
					// There isn't a reliable way to get eclipse artifacts except through p2 repositories,
					// which gradle does not yet support.  For now we're forcing it with ivy.
					url extension.updateSite()
					layout "pattern", {
						artifact "plugins/[artifact]_[revision].[ext]"
					}
				}
			}

			project.dependencies {
				for (dep in SwtExtension.DEPS) {
					compile extension.fullDep(dep);
				}
			}
		}
	}
}
