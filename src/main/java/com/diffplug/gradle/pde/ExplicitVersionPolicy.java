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
package com.diffplug.gradle.pde;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckReturnValue;

import org.osgi.framework.Version;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.common.collect.Immutables;
import com.diffplug.common.collect.Maps;
import com.diffplug.gradle.Lazyable;

/** Specifies a policy for which bundles we will keep multiple versions of, used in {@link PdeBuildTask}. */
public class ExplicitVersionPolicy {
	/** A map from plugin name to the list of versions that are okay to resolve with the first entry. */
	private Map<String, Resolve> resolvable = Maps.newHashMap();

	/**
	 * Specifies that we expect multiple versions of the given plugin,
	 * the return value must be used to set the versions that will be kept.
	 */
	@CheckReturnValue
	public Resolve resolve(String pluginName, String... versions) {
		Resolve mapping = new Resolve(versions);
		resolvable.put(pluginName, mapping);
		return mapping;
	}

	/** Creates a deep copy of this ExplicitVersionPolicy. */
	public ExplicitVersionPolicy copy() {
		ExplicitVersionPolicy clone = new ExplicitVersionPolicy();
		resolvable.forEach((key, resolve) -> clone.resolvable.put(key, new Resolve(resolve)));
		return clone;
	}

	/**
	 * Represents a given plugin and its input versions,
	 * and specifies the versions to use when resolving it.
	 */
	public static class Resolve {
		private final ImmutableSet<Version> accepts;
		private ImmutableSet<Version> takes;

		private Resolve(String... accepts) {
			Preconditions.checkArgument(accepts.length > 1, "Only applies for plugins with multiple versions.");
			this.accepts = parse(accepts);
		}

		private Resolve(Resolve source) {
			this.accepts = source.accepts;
			this.takes = source.takes;
		}

		public void with(String... takes) {
			this.takes = parse(takes);
			Preconditions.checkArgument(accepts.containsAll(this.takes),
					"Takes %s must be a strict subset of accepts %s.", takes, accepts);
		}

		public void withFirst() {
			this.takes = ImmutableSet.of(accepts.asList().get(0));
		}

		static ImmutableSet<Version> parse(String[] raw) {
			return Arrays.asList(raw).stream().map(Version::parseVersion).collect(Immutables.toSet());
		}
	}

	/** Returns the version for the given plugin. */
	Set<Version> useVersions(String plugin, Set<Version> present) {
		if (present.size() == 1) {
			if (resolvable.containsKey(plugin)) {
				throw new IllegalArgumentException("Expected " + resolvable.get(plugin).accepts + " for '" + plugin + "', but had only " + present);
			} else {
				return present;
			}
		} else if (present.isEmpty()) {
			throw new IllegalArgumentException("No such plugin: " + plugin);
		} else {
			Resolve mapping = resolvable.get(plugin);
			if (mapping == null) {
				throw new IllegalArgumentException("Conflicting versions for '" + plugin + "'!  Had " + present + ", call resolve(name, existingVersions).with(versionsToKeep)");
			} else {
				if (mapping.accepts.equals(present)) {
					return mapping.takes;
				} else {
					throw new IllegalArgumentException("Conflicts don't match for '" + plugin + "'!  Suggested resolution was " + mapping.accepts + ", but available was " + present);
				}
			}
		}
	}

	/** Creates a Lazyable ExplicitVersionPolicy. */
	static Lazyable<ExplicitVersionPolicy> createLazyable() {
		return new Lazyable<>(new ExplicitVersionPolicy(), ExplicitVersionPolicy::copy);
	}
}
