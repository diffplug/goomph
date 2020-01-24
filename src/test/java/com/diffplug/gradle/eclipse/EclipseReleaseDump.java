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


import com.diffplug.gradle.pde.EclipseRelease;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EclipseReleaseDump {
	public static void main(String[] args) {
		List<String> releases = Arrays.asList(
				"4.6.2",
				"4.6.3",
				"4.7.0",
				"4.7.1",
				"4.7.1.a",
				"4.7.2",
				"4.7.3",
				"4.7.3.a",
				"4.8.0",
				"4.9.0",
				"4.10.0",
				"4.11.0",
				"4.12.0",
				"4.13.0",
				"4.14.0");
		for (String release : releases) {
			Map<String, String> versions = MavenCentralMapping.bundleToVersion(EclipseRelease.official(release));
			System.out.println("release=" + release + " swt=" + versions.get("org.eclipse.swt") + " jface=" + versions.get("org.eclipse.jface"));
		}
	}
}
