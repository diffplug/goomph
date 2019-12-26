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

/** Utilities for getting environment variables in buildscripts. */
public class EnvMisc {
	/** Returns the given environment variable, throwing a descriptive error if it doesn't exist. */
	public static String get(String key, String whatItIs) {
		String value = System.getenv(key);
		if (value != null) {
			return value;
		} else {
			throw new IllegalArgumentException("You must set environment variable '" + key + "' to " + whatItIs);
		}
	}

	/** Returns the given environment variable, printing a descriptive warning and using a default value if it doesn't exist. */
	public static String getOptional(String key, String defaultValue, String whatItIs) {
		String value = System.getenv(key);
		if (value != null) {
			return value;
		} else {
			System.err.println("You should set environment variable '" + key + "' to " + whatItIs);
			System.err.println("    defaulting to " + defaultValue);
			return defaultValue;
		}
	}
}
