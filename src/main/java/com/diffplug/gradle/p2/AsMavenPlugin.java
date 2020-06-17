/*
 * Copyright (C) 2015-2020 DiffPlug
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


import com.diffplug.common.base.Errors;
import com.diffplug.gradle.LegacyPlugin;
import com.diffplug.gradle.ProjectPlugin;
import org.gradle.api.Project;

/**
 * Downloads a set of artifacts from a p2 repository
 * and stuffs them into a local maven repository in the
 * `build/p2asmaven` folder, then adds this repository
 * to the project.
 *
 * ```groovy
 * apply plugin: 'com.diffplug.p2.asmaven'
 * p2AsMaven {
 *     // the maven group they shall have
 *     group 'eclipse-deps', {
 *         // the repositories and artifacts to download
 *         repoEclipse '4.5.2'
 *         iu          'org.eclipse.jdt.core'
 *     }
 * }
 *
 * dependencies {
 *     compile 'eclipse-deps:org.eclipse.jdt.core:+'
 * }
 * ```
 * 
 * You can also specify multiple groups, as such
 * 
 * ```groovy
 * def SUPPORTED_VERSIONS = ['4.4', '4.5', '4.6']
 * p2AsMaven {
 *     for (version in SUPPORTED_VERSIONS) {
 *         group 'eclipse-deps-' + version, {
 *             repoEclipse version
 *             iu 'org.eclipse.swt'
 *             // this will cause any OSGi jars which ask to be expanded into
 *             // folders to be exploded, e.g. Eclipse-BundleShape: dir 
 *             repo2runnable()
 *         }
 *     }
 * }
 * 
 * // build folder will look like this:
 * build/p2AsMaven/
 *     maven/
 *         eclipse-deps-4.4/
 *         eclipse-deps-4.5/
 *         eclipse-deps-4.6/
 *     p2/
 *         eclipse-deps-4.4/
 *         eclipse-deps-4.5/
 *         eclipse-deps-4.6/
 *     p2runnable/
 *         eclipse-deps-4.4/
 *         eclipse-deps-4.5/
 *         eclipse-deps-4.6/
 * ```
 * 
 * The maven repository does not contain any dependency information,
 * just the raw jars.  In the example above, when p2 downloads
 * `org.eclipse.jdt.core`, it also downloads all of its dependencies.
 * But none of these dependencies are added automatically - you have to
 * add them yourself.
 * 
 * ## Example projects
 * 
 * * [spotless](https://github.com/diffplug/spotless)
 * * (send us yours in a [PR](https://github.com/diffplug/goomph)!)
 * 
 * ## Proxy support
 * 
 * If you need to pass through a proxy, you'll need to create a file 
 * called `proxies.ini` with content like this:
 * 
 * ```ini
 * org.eclipse.core.net/proxyData/HTTP/host=someproxy.ericsson.se
 * org.eclipse.core.net/proxyData/HTTPS/host=someproxy.ericsson.se
 * org.eclipse.core.net/proxyData/HTTPS/hasAuth=false
 * org.eclipse.core.net/proxyData/HTTP/port=8080
 * org.eclipse.core.net/proxyData/HTTPS/port=8080
 * org.eclipse.core.net/org.eclipse.core.net.hasMigrated=true
 * org.eclipse.core.net/nonProxiedHosts=*.ericsson.com|127.0.0.1
 * org.eclipse.core.net/systemProxiesEnabled=false
 * org.eclipse.core.net/proxyData/HTTP/hasAuth=false 
 * ```
 * 
 * Once you've done this, add this to your `build.gradle`:
 * 
 * ```groovy
 * p2AsMaven {
 *     group 'myGroup', {
 *         ...
 *         p2ant {
 *             it.addArg('plugincustomization', '<path to proxies.ini>')
 *         }
 *     }
 * }
 * ```
 * 
 * If you think this is too hard, vote for [this issue on GitHub](https://github.com/diffplug/goomph/issues/12)
 * and [this bug on eclipse](https://bugs.eclipse.org/bugs/show_bug.cgi?id=382875) and we can make it easier.
 *
 * ## Slicing options
 *
 * You can control how the iu's are resolved using
 *
 * ```groovy
 * p2AsMaven {
 *   group 'eclipse-deps', {
 *     ...
 *     slicingOption 'latestVersionOnly', 'true'
 *   }
 * }
 * ```
 * 
 * You can see all the available slicing options [here](https://wiki.eclipse.org/Equinox/p2/Ant_Tasks#SlicingOptions).
 *
 * ## Append option
 *
 * You can control whether iu's should be appended to destination repository
 * true: already downloaded iu's are preserved, new iu's are downloaded into the existing repo
 * false: Default value for goomph, repository will be completely cleared before download new iu's
 *
 * ```groovy
 * p2AsMaven {
 *   group 'eclipse-deps', {
 *     ...
 *     append true
 *   }
 * }
 * ```
 *
 * More info [here](https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm).
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
	public static class Legacy extends LegacyPlugin {
		public Legacy() {
			super(AsMavenPlugin.class, "com.diffplug.p2.asmaven");
		}
	}

	AsMavenExtension extension;

	@Override
	protected void applyOnce(Project project) {
		LegacyPlugin.applyForCompat(project, Legacy.class);
		extension = project.getExtensions().create(AsMavenExtension.NAME, AsMavenExtension.class, project);
		project.afterEvaluate(proj -> {
			// reload
			Errors.rethrow().run(extension::run);
			// set maven repo
			project.getRepositories().maven(maven -> {
				maven.setUrl(extension.mavenDir(proj));
				maven.metadataSources(sources -> {
					sources.mavenPom();
					sources.artifact();
				});
			});
		});
	}

	public AsMavenExtension extension() {
		return extension;
	}
}
