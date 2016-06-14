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
public enum EclipseRelease {
	R_4_5_2;

	/** Returns the latest released version of eclipse which is supported by Goomph. */
	public static EclipseRelease latest() {
		return R_4_5_2;
	}

	/** Returns the eclipse update site for this release. */
	public String updateSite() {
		String root = "http://download.eclipse.org/eclipse/updates/";
		// @formatter:off
		switch (this) {
		case R_4_5_2: return root + "4.5/R-4.5.2-201602121500/";
		default: throw Unhandled.enumException(this);
		}
		// @formatter:on
	}

	/** Returns the OSGi version for this release. */
	public Version version() {
		return Version.valueOf(toString());
	}

	@Override
	public String toString() {
		return name().substring(2).replace('_', '.');
	}
}
