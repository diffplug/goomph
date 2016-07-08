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

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.FileMisc;

/** DSL for {@link AsMavenPlugin}. */
public class AsMavenExtension {
	public static final String NAME = "p2AsMaven";

	Object destination = "build/p2asmaven";
	final LinkedHashMap<String, Action<AsMavenGroup>> groups = new LinkedHashMap<>();

	/** Sets the destination directory, defaults to `build/p2asmaven`. */
	public void destination(Object destination) {
		this.destination = Objects.requireNonNull(destination);
	}

	/** Creates a maven group which will be populated by the given action. */
	public void group(String mavenGroup, Action<AsMavenGroup> action) {
		Object previous = groups.put(mavenGroup, action);
		if (previous != null) {
			throw new IllegalArgumentException("Duplicate groups for " + mavenGroup);
		}
	}

	void run(Project project) {
		Set<File> files = new HashSet<>();
		File p2asmaven = project.file(destination);
		groups.forEach((group, action) -> {
			// populate the def
			AsMavenGroup def = new AsMavenGroup(group);
			action.execute(def);
			// run it
			AsMavenGroupImpl impl = Errors.rethrow().get(() -> def.run(project, p2asmaven));
			// keep track of what is clean
			files.add(impl.dirP2());
			files.add(impl.dirP2Runnable());
			files.add(impl.dirMavenGroup());
			files.add(impl.tokenFile());
		});
		// delete the other files
		deleteStragglers(p2asmaven, files, AsMavenGroupImpl.SUBDIR_P2, AsMavenGroupImpl.SUBDIR_P2_RUNNABLE, AsMavenGroupImpl.SUBDIR_MAVEN);
	}

	private void deleteStragglers(File root, Set<File> toKeep, String... dirs) {
		for (String dir : dirs) {
			File dirRoot = new File(root, dir);
			if (dirRoot.exists()) {
				for (File file : FileMisc.list(dirRoot)) {
					if (!toKeep.contains(file)) {
						Errors.log().run(() -> {
							FileMisc.forceDelete(file);
						});
					}
				}
			}
		}
	}

	File mavenDir(Project project) {
		return new File(project.file(destination), AsMavenGroupImpl.SUBDIR_MAVEN);
	}
}
