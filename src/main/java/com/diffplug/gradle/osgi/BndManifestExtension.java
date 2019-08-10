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
package com.diffplug.gradle.osgi;

import java.util.*;

import org.gradle.api.plugins.JavaPlugin;

import com.diffplug.common.collect.Iterables;

/** Determines where the manifest is written out by {@link BndManifestPlugin}. */
public class BndManifestExtension {
	public String copyFromTask = JavaPlugin.JAR_TASK_NAME;

	public void copyFromTask(String copyFromTask) {
		this.copyFromTask = copyFromTask;
	}

	public Object copyTo = null;

	public void copyTo(Object copyTo) {
		this.copyTo = copyTo;
	}

	public boolean mergeWithExisting = false;

	public void mergeWithExisting(boolean mergeWithExisting) {
		this.mergeWithExisting = mergeWithExisting;
	}

	public Set<Object> includeTasks = new HashSet<>(Collections.singletonList(JavaPlugin.JAR_TASK_NAME));

	public void includeTask(Object task) {
		includeTasks.add(task);
	}

	public void includeTasks(Object... tasks) {
		Collections.addAll(includeTasks, tasks);
	}

	public void setIncludeTasks(Iterable<?> includeTasks) {
		this.includeTasks.clear();
		includeTasks(Iterables.toArray(includeTasks, Object.class));
	}

	static final String NAME = "osgiBndManifest";
}
