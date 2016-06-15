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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.ListMultimap;
import com.diffplug.common.collect.Multimaps;

/**
 * Utility class which builds eclipse arguments.
 *
 * ```java
 * EclipseArgsBuilder builder = new EclipseArgsBuilder();
 * builder.addArg("prop", "a");
 * builder.addArg("prop", "b");
 * builder.addArg("flag");
 *
 * System.out.println(Joiner.on(" ").join(builder.toArgList()))
 * -prop a,b -flag
 * ```
 */
public class EclipseArgsBuilder {
	private ListMultimap<String, String> args = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);

	/**
	 * Any cached data used by the OSGi framework and eclipse runtime will be wiped clean. This will clean the caches used to store bundle dependency resolution and eclipse extension registry data. Using this option will force eclipse to reinitialize these caches.
	 */
	public void clean() {
		addArg("clean");
	}

	/**
	 * Any log output is also sent to Java's System.out (typically back to the command shell if any).
	 */
	public void consolelog() {
		addArg("consolelog");
	}

	/**
	 * The identifier of the application to run.
	 */
	public void application(String application) {
		addArg("application", application);
	}

	public void addArg(String key) {
		// no reason to set a flag twice
		if (!args.get(key).equals(Collections.singletonList(""))) {
			args.put(key, "");
		}
	}

	public void addArg(String key, String value) {
		args.put(key, value);
	}

	public List<String> toArgList() {
		List<String> argList = new ArrayList<>(args.size() * 2);
		args.asMap().forEach((key, values) -> {
			argList.add("-" + key);
			boolean valuesIsEmptyString = values.size() == 1 && Iterables.getOnlyElement(values).isEmpty();
			if (!valuesIsEmptyString) {
				argList.add(Joiner.on(",").join(values));
			}
		});
		return argList;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		args.asMap().forEach((key, values) -> {
			builder.append("-" + key);
			boolean valuesIsEmptyString = values.size() == 1 && Iterables.getOnlyElement(values).isEmpty();
			if (!valuesIsEmptyString) {
				builder.append(' ');
				builder.append(Joiner.on(",").join(values));
			}
			builder.append('\n');
		});
		return builder.toString();
	}
}
