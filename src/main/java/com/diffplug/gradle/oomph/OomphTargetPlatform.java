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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;

import com.diffplug.gradle.FileMisc;

/** DSL for {@link OomphIdePlugin}. */
public class OomphTargetPlatform {
	final Project project;

	OomphTargetPlatform(Project project) {
		this.project = project;
	}

	List<Object> installations = new ArrayList<>();

	/** Adds an installation. */
	public void installation(Object installation) {
		installations.add(installation);
	}

	/** Returns a list of the root installations in this project. */
	List<File> getInstallations() {
		return FileMisc.parseListFile(project, installations);
	}

	@Override
	public String toString() {
		return getInstallations().toString();
	}
}
