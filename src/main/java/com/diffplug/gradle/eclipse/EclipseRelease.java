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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.gradle.internal.impldep.com.google.common.collect.ImmutableSet;
import org.osgi.framework.Version;

import com.diffplug.common.base.Unhandled;

/**
 * Enum representing all the released versions of eclipse from 4.5.2 onwards.
 *
 * If an enum is present here, then Goomph promises the following about it:
 *
 * - You can run PDE tasks using the given release
 * - There is a goomph-p2-bootstrap based on the given release
 */
public final class EclipseRelease {
	final String id;
	final Version version;
	final String updateSite;

	private EclipseRelease(String id, Version version, String updateSite) {
		this.id = id;
		this.version = version;
		this.updateSite = updateSite;
	}

	/** Creates a custom eclipse release (use an official release, e.g. `EclipseRelease.R_4_5_2` whenever possible). */
	public static EclipseRelease createWithIdVersionUpdatesite(String id, String version, String updateSite) {
		Optional<EclipseRelease> officialConflict = OFFICIAL.stream().filter(release -> release.id.equals(id)).findFirst();
		if (officialConflict.isPresent()) {
			throw new IllegalArgumentException("User-generated version cannot conflict with built-in " + officialConflict.get() + ", use EclipseRelease.R_" + id.replace(".", "_"));
		}
		return new EclipseRelease(
				Objects.requireNonNull(id),
				Version.valueOf(version),
				Objects.requireNonNull(updateSite));
	}

	/** Mars.2 */
	public static final EclipseRelease R_4_5_2 = officialRelease("4.5.2");

	/** The officially released versions which are supported by goomph. */
	public static final Set<EclipseRelease> OFFICIAL = ImmutableSet.of(R_4_5_2);

	/** Returns the latest officially released version which is supported by Goomph. */
	public static EclipseRelease latestOfficial() {
		return R_4_5_2;
	}

	private static EclipseRelease officialRelease(String version) {
		Function<String, String> updateSite = v -> {
			String root = "http://download.eclipse.org/eclipse/updates/";
			// @formatter:off
			switch (v) {
			case "4.5.2": return root + "4.5/R-4.5.2-201602121500/";
			default: throw Unhandled.stringException(v);
			}
		};
		return new EclipseRelease(version, Version.valueOf(version), updateSite.apply(version));
	}

	/** Returns the OSGi version for this release. */
	public Version version() {
		return version;
	}

	/** Returns the update site for this release. */
	public String updateSite() {
		return updateSite;
	}

	/** Returns the id. */
	@Override
	public String toString() {
		return id;
	}
}
