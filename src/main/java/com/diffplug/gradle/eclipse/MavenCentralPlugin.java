/*
 * Copyright (C) 2018-2020 DiffPlug
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
package com.diffplug.gradle.eclipse;


import com.diffplug.gradle.LegacyPlugin;
import com.diffplug.gradle.ProjectPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

/**
 * Now that Eclipse is publishing artifacts to maven central,
 * it's possible to use the valuable components of the
 * eclipse project without getting bogged down in p2 and OSGi.
 * 
 * The trouble is that it can be difficult to get a coherent set
 * of versions across the many bundles.  It's also a little tricky
 * to know what the groupId:artifactId mapping is, since eclipse
 * artifacts only have a bundleId by default.
 * 
 * Here is the fix:
 * 
 * ```
 * apply plugin: 'com.diffplug.eclipse.mavencentral'
 * eclipseMavenCentral {
 *     release '4.7.0', {
 *         // supports the standard java configurations
 *         api 'org.eclipse.swt'
 *         implementation 'org.eclipse.jdt'
 *         testImplementation 'org.eclipse.swt'
 *         // and custom ones too
 *         dep 'implementation', 'org.eclipse.jdt'
 *         // specify this to add the native jars for this platform
 *         useNativesForRunningPlatform()
 *         // specify that all transitive dependencies should be from this release
 *         // (otherwise the version ranges which eclipse uses will use latest available)
 *         constrainTransitivesToThisRelease()
 *     }
 * }
 * ```
 * 
 * ## Platform-native jars
 * 
 * When an eclipse jar needs a platform-specific dependency,
 * such as SWT, the platform-specific part of the name is specified
 * in the POM as `${osgi.platform}`.  useNativesForRunningPlatform()
 * will replace `${osgi.platform}` with whatever is appropriate for
 * your platform, such as `org.eclipse.swt.win32.win32.x86_64`.
 * 
 * That's normally all you need, but if you want more specific
 * control, there is a special `sourceSetNative` method,
 * along with builtins like `testRuntimeNative` for each
 * of the builtin java configurations.
 * 
 * ```
 * eclipseMavenCentral {
 *     release '4.7.0', {
 *         testRuntimeOnlyNative 'org.eclipse.swt'
 *         nativeDep 'testRuntimeOnly', 'org.eclipse.swt'
 *     }
 * }
 * ```
 * 
 * Either of the above lines will add the 
 * `org.eclipse.swt.win32.win32.x86_64` as a `testRuntimeOnly`
 * dependency if you're running on a 64-bit JVM on Windows, or some
 * whichever platform-specific jar is appropriate.  This
 * works only for the SWT naming convention, `windowing.os.arch`.
 * 
 * ## Compatibility
 * 
 * Only works with versions from `4.6.2` onwards.
 * 
 */
public class MavenCentralPlugin extends ProjectPlugin {
	public static class Legacy extends LegacyPlugin {
		public Legacy() {
			super(MavenCentralPlugin.class, "com.diffplug.eclipse.mavencentral");
		}
	}

	@Override
	protected void applyOnce(Project project) {
		LegacyPlugin.applyForCompat(project, Legacy.class);
		project.getPlugins().apply(JavaPlugin.class);
		project.getExtensions().create(MavenCentralExtension.NAME, MavenCentralExtension.class, project);
	}
}
