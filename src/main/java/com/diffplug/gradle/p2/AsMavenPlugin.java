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

/**
 * Downloads a set of artifacts from a p2 repository
 * and stuffs them into a local maven repository in the
 * `build/p2asmaven` folder, then adds this repository
 * to the project.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.gradle.p2.asmaven'
 * p2AsMaven {
 *     // the maven group they shall have
 *     mavenGroup 'p2asmaven'
 *     // the repositories and artifacts to download
 *     p2.addRepo(ECLIPSE_UPDATE_SITE)
 *     p2.addIU('org.eclipse.jdt.core')

 *     // copies all artifacts to the `p2runnable` folder in a "runnable" form
 *     // appropriate for target platforms and PDE build
 *     // e.g. org.eclipse.equinox.launcher will be extracted.
 *     repo2runnable()
 * }
 *
 * dependencies {
 *     compile 'eclipse:org.eclipse.jdt.core:+'
 *     compile 'eclipse:org.eclipse.text:+'
 * }
 * ```
 *
 * The `p2` property inside `p2AsMaven` is a {@link P2Model}.
 *
 * The maven repository does not contain any dependency information,
 * just the raw jars.  In the example above, when p2 downloads
 * `org.eclipse.jdt.core`, it also downloads all of its dependencies.
 * But none of these dependencies are added automatically - you have to
 * add them yourself.
 *
 * See [spotless](https://github.com/diffplug/spotless) for a production
 * example.
 *
 * ## Acknowledgements and comparisons to other options
 *
 * Inspired by Andrey Hihlovskiy's [unpuzzle](https://github.com/akhikhl/unpuzzle).
 * Unpuzzle downloads a zipped-up copy of the eclipse IDE and stuffs its jars into a
 * maven repository.  Additionally, it parses out the `Require-Bundle` directives to
 * put partial dependency information in the maven metadata.
 *
 * Simon Templer has a [fork](https://github.com/stempler/unpuzzle) which supports parsing
 * the `Import-Package` and `Export-Package` directive to build even more detailed
 * dependency information.
 *
 * A third option is Dr. David Akehurst's [p2 dependency resolver](https://github.com/dhakehurst/net.akehurst.build.gradle),
 * which is a partial re-implementation of P2, and uses internal gradle APIs to hook into
 * Gradle's dependency resolution.  It's very impressive, but it has a few limitations:
 *
 * - It does not support composite repositories
 * - It does not work on Gradle > 2.8
 *
 * All of the above are great work, and served as useful examples.  The central
 * problem which is the cause of so many not-quite-satisfying implementations is this:
 *
 * - Using the real p2 is cumbersome, but possible.
 * - Re-implementing the first 70% of p2 is easy, but the last 30% is hard and moving (they're still adding features).
 * - Gradle doesn't yet have an extensible API for repositories.
 *
 * In Goomph, we will always use the real p2.  While we work around limitations in
 * gradle and p2, we will only use public APIs in both products.
 */
public class AsMavenPlugin extends ProjectPlugin {
	@Override
	protected void applyOnce(Project project) {
		AsMavenExtension extension = project.getExtensions().create(AsMavenExtension.NAME, AsMavenExtension.class, project);
		project.afterEvaluate(proj -> {
			Errors.rethrow().run(extension.mavenify::run);
			project.getRepositories().maven(maven -> {
				maven.setUrl(extension.mavenify.getDestinationMaven());
			});
		});
	}
}
