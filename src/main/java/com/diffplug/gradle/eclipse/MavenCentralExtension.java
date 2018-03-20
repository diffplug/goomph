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
package com.diffplug.gradle.eclipse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.pde.EclipseRelease;

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

		public void compile(String bundleId) {
			dep(JavaPlugin.COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void testCompile(String bundleId) {
			dep(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void runtime(String bundleId) {
			dep(JavaPlugin.RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void testRuntime(String bundleId) {
			dep(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void dep(String configName, String bundleId) {
			String groupIdArtifactId = MavenCentralMapping.groupIdArtifactId(bundleId);
			project.getDependencies().add(configName, groupIdArtifactId + ":" + bundleToVersion.get(bundleId));
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

		public void compileNative(String bundleId) {
			nativeDep(JavaPlugin.COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void testCompileNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME, bundleId);
		}

		public void runtimeNative(String bundleId) {
			nativeDep(JavaPlugin.RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void testRuntimeNative(String bundleId) {
			nativeDep(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME, bundleId);
		}

		public void nativeDep(String configName, String bundleId) {
			SwtPlatform platform = SwtPlatform.getRunning();
			dep(configName, bundleId + "." + platform);
		}
	}
}
