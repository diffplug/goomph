/*
 * Copyright 2019 DiffPlug
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
package com.diffplug.gradle.eclipserunner;

import java.util.List;

/**
 * Runs the given args using a headless eclipse instance.
 *
 * The major implementations are:
 *
 * - {@link NativeRunner} for running against a native launcher (eclipsec.exe).
 * - {@link JarFolderRunner} for running within this JVM against a folder of jars.
 * - {@link JarFolderRunnerExternalJvm} for running outside this JVM against a folder of jars.
 */
public interface EclipseRunner {
	/** Runs the eclipse instance with the given arguments. */
	void run(List<String> args) throws Exception;
}
