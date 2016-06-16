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

import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.ProjectPlugin;

public class AsMavenPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		AsMavenExtension extension = project.getExtensions().create(AsMavenExtension.NAME, AsMavenExtension.class, project);
		project.afterEvaluate(proj -> {
			Errors.rethrow().run(extension.mavenify::run);
			project.getRepositories().maven(maven -> {
				maven.setUrl(extension.mavenify.getDestination());
			});
		});
	}
}
