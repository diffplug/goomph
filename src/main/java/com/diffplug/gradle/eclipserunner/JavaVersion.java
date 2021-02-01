/*
 * Copyright (C) 2021 DiffPlug
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
package com.diffplug.gradle.eclipserunner;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to easily check java versions
 */
public class JavaVersion {

	private static final Pattern REGEX = Pattern.compile("([1-9]\\d*)\\.(\\d+)\\.(\\d+)(_(\\d+))?");

	private int major;
	private int minor;
	private int fix;

	private JavaVersion(String major, String minor, String fix) {
		this.major = Integer.parseInt(major);
		this.minor = Integer.parseInt(minor);
		this.fix = Integer.parseInt(fix);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getFix() {
		return fix;
	}

	public boolean isGreaterOrEqual(String other) {
		return isGreaterOrEqual(JavaVersion.fromString(other));
	}

	public boolean isGreaterOrEqual(JavaVersion other) {
		if (major < other.major) {
			return false;
		} else if (major > other.major) {
			return true;
		} else if (minor < other.minor) {
			return false;
		} else if (minor > other.minor) {
			return true;
		}
		// minor numbers are equivalent so check service
		return fix >= other.fix;
	}

	public static JavaVersion fromString(String v) {
		final Matcher matcher = REGEX.matcher(v);
		if (matcher.matches()) {
			return new JavaVersion(matcher.group(1), matcher.group(2), matcher.group(3));
		}
		return null;
	}

}
