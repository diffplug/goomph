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
package com.diffplug.gradle.eclipse;


import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.pde.EclipseRelease;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.plugins.JavaPlugin;

public class MavenCentralExtension {
	public static final String NAME = "eclipseMavenCentral";

	private final Project project;

	public MavenCentralExtension(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	public void release(String version, Action<ReleaseConfigurer> configurer) throws IOException {
		release(EclipseRelease.official(version), configurer);
	}

	public void release(EclipseRelease release, Action<ReleaseConfigurer> configurer) throws IOException {
		configurer.execute(new ReleaseConfigurer(release));
	}

	public class ReleaseConfigurer {
		final EclipseRelease release;
		final Map<String, String> bundleToVersion;

		public ReleaseConfigurer(EclipseRelease release) throws IOException {
			this.release = release;
			this.bundleToVersion = MavenCentralMapping.bundleToVersion(release);
		}

		public void compileOnly(String bundleId) {
			dep(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, bundleId);
		}

		public void testCompileOnly(String bundleId) {
			dep(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void compile(String bundleId) {
			dep(JavaPlugin.COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void api(String bundleId) {
			dep(JavaPlugin.API_CONFIGURATION_NAME, bundleId);
		}

		public void implementation(String bundleId) {
			dep(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void testCompile(String bundleId) {
			dep(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void testImplementation(String bundleId) {
			dep(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void runtime(String bundleId) {
			dep(JavaPlugin.RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void runtimeOnly(String bundleId) {
			dep(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void testRuntime(String bundleId) {
			dep(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void testRuntimeOnly(String bundleId) {
			dep(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, bundleId);
		}

		public void dep(String configName, String bundleId) {
			String groupIdArtifactId = MavenCentralMapping.groupIdArtifactId(bundleId);
			project.getDependencies().add(configName, groupIdArtifactId + ":" + bundleToVersion.get(bundleId));
		}

		private static final String $_OSGI_PLATFORM = "${osgi.platform}";

		public void useNativesForRunningPlatform() {
			project.getConfigurations().all(config -> {
				config.getResolutionStrategy().eachDependency(details -> {
					ModuleVersionSelector req = details.getRequested();
					if (req.getName().contains($_OSGI_PLATFORM)) {
						String running = SwtPlatform.getRunning().toString();
						details.useTarget(req.getGroup() + ":" + req.getName().replace($_OSGI_PLATFORM, running) + ":" + req.getVersion());
					}
				});
			});
		}

		/////////////
		// natives //
		/////////////
		public void compileOnlyNative(String bundleId) {
			nativeDep(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, bundleId);
		}

		public void testCompileOnlyNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void compileNative(String bundleId) {
			nativeDep(JavaPlugin.COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void apiNative(String bundleId) {
			nativeDep(JavaPlugin.API_CONFIGURATION_NAME, bundleId);
		}

		public void implementationNative(String bundleId) {
			nativeDep(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void testCompileNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void testImplementationNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void runtimeNative(String bundleId) {
			nativeDep(JavaPlugin.RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void runtimeOnlyNative(String bundleId) {
			nativeDep(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, bundleId);
		}

		@Deprecated
		public void testRuntimeNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void testRuntimeOnlyNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, bundleId);
		}

		public void nativeDep(String configName, String bundleId) {
			SwtPlatform platform = SwtPlatform.getRunning();
			dep(configName, bundleId + "." + platform);
		}
	}
}
