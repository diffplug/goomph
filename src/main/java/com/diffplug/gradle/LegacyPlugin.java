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
package com.diffplug.gradle;


import com.diffplug.common.collect.Maps;
import java.util.Map;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.SingleMessageLogger;

public class LegacyPlugin implements Plugin<Project> {
	private final Class<? extends Plugin<Project>> newPlugin;
	private final String newId;

	protected LegacyPlugin(Class<? extends Plugin<Project>> newPlugin, String newPluginId) {
		this.newPlugin = newPlugin;
		this.newId = newPluginId;
	}

	@Override
	public void apply(Project proj) {
		Map.Entry<Integer, Class<? extends LegacyPlugin>> cacheEntry = Maps.immutableEntry(System.identityHashCode(proj), getClass());
		if (cacheEntry.equals(clazzBeingCompatApplied.get())) {
			clazzBeingCompatApplied.set(null);
			return;
		}
		proj.getPlugins().apply(newPlugin);
		String oldId;
		if (newId.equals("com.diffplug.osgi.equinoxlaunch")) {
			oldId = "com.diffplug.gradle.equinoxlaunch";
		} else {
			oldId = newId.replace("com.diffplug.", "com.diffplug.gradle.");
		}
		SingleMessageLogger.nagUserOfReplacedPlugin(oldId, newId);
	}

	public static void applyForCompat(Project proj, Class<? extends LegacyPlugin> clazz) {
		clazzBeingCompatApplied.set(Maps.immutableEntry(System.identityHashCode(proj), clazz));
		proj.getPlugins().apply(clazz);
	}

	private static ThreadLocal<Map.Entry<Integer, Class<? extends LegacyPlugin>>> clazzBeingCompatApplied = new ThreadLocal<>();
}
