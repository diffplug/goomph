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
package com.diffplug.gradle;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.gradle.api.Project;

import com.diffplug.common.base.StringPrinter;

/** Helper for generating a state string. */
public class StateBuilder {
	final Project project;
	final SortedMap<String, String> map = new TreeMap<>();

	public StateBuilder(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	public void add(String key, @Nullable Object value) {
		map.put(key, Objects.toString(value));
	}

	public void addFile(String key, @Nullable Object value) {
		add(key, value == null ? null : project.file(value));
	}

	@Override
	public String toString() {
		return StringPrinter.buildString(printer -> {
			map.forEach((key, value) -> {
				printer.println(key + ": " + value);
			});
		});
	}
}
