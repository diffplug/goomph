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

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osgi.framework.Version;

/**
 * Models an Eclipse release, such as Mars SR2.
 *
 * Supports all official releases from 3.5.0 to present (currently 4.6.0).
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
		EclipseRelease official = officialReleaseMaybe(id);
		if (official != null) {
			throw new IllegalArgumentException("User-generated version cannot conflict with built-in " + id + ", change the ID or use EclipseRelease.forVersion(" + id + ")");
		}
		return new EclipseRelease(
				Objects.requireNonNull(id),
				Version.parseVersion(version),
				Objects.requireNonNull(updateSite));
	}

	/** Returns the given officially released version which is supported by Goomph. */
	public static EclipseRelease official(String version) {
		EclipseRelease release = officialReleaseMaybe(version);
		if (release == null) {
			throw new IllegalArgumentException(version + " is not supported.  We only support " + supportedRange());
		} else {
			return release;
		}
	}

	/** Returns a message describing the currently supported range of versions. */
	public static String supportedRange() {
		return "3.5.0 through 4.5.2";
	}

	@Nullable
	private static EclipseRelease officialReleaseMaybe(String version) {
		Function<String, String> updateSiteFunc = v -> {
			// @formatter:off
			String root = "http://download.eclipse.org/eclipse/updates/";
			switch (v) {
			case "3.5.0": return root + "3.5/R-3.5-200906111540/";
			case "3.5.1": return root + "3.5/R-3.5.1-200909170800/";
			case "3.5.2": return root + "3.5/R-3.5.2-201002111343/";
			case "3.6.0": return root + "3.6/R-3.6-201006080911/";
			case "3.6.1": return root + "3.6/R-3.6.1-201009090800/";
			case "3.6.2": return root + "3.6/R-3.6.2-201102101200/";
			case "3.7.0": return root + "3.7/R-3.7-201106131736/";
			case "3.7.1": return root + "3.7/R-3.7.1-201109091335/";
			case "3.7.2": return root + "3.7/R-3.7.2-201202080800/";
			case "3.8.0": return root + "3.8/R-3.8-201206081200/";
			case "3.8.1": return root + "3.8/R-3.8.1-201209141540/";
			case "3.8.2": return root + "3.8/R-3.8.2-201301310800/";
			case "4.2.0": return root + "4.2/R-4.2-201206081400/";
			case "4.2.1": return root + "4.2/R-4.2.1-201209141800/";
			case "4.2.2": return root + "4.2/R-4.2.2-201302041200/";
			case "4.3.0": return root + "4.3/R-4.3-201306052000/";
			case "4.3.1": return root + "4.3/R-4.3.1-201309111000/";
			case "4.3.2": return root + "4.3/R-4.3.2-201402211700/";
			case "4.4.0": return root + "4.4/R-4.4-201406061215/";
			case "4.4.1": return root + "4.4/R-4.4.1-201409250400/";
			case "4.4.2": return root + "4.4/R-4.4.2-201502041700/";
			case "4.5.0": return root + "4.5/R-4.5-201506032000/";
			case "4.5.1": return root + "4.5/R-4.5.1-201509040015/";
			case "4.5.2": return root + "4.5/R-4.5.2-201602121500/";
			case "4.6.0": return root + "4.6/R-4.6-201606061100/";
			default: return null;
			}
			// @formatter:on
		};
		String updateSite = updateSiteFunc.apply(version);
		if (updateSite == null) {
			return null;
		} else {
			return new EclipseRelease(version, Version.parseVersion(version), updateSite);
		}
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

	/** Returns the hashCode for this release. */
	@Override
	public int hashCode() {
		return Objects.hash(id, version, updateSite);
	}

	@Override
	public boolean equals(Object otherObj) {
		if (otherObj instanceof EclipseRelease) {
			EclipseRelease other = (EclipseRelease) otherObj;
			return id.equals(other.id)
					&& version.equals(other.version)
					&& updateSite.equals(other.updateSite);
		} else {
			return false;
		}
	}
}
