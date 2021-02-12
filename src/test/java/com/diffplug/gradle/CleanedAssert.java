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
package com.diffplug.gradle;


import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Assert;

public class CleanedAssert {
	public static void assumeJre8() {
		org.junit.Assume.assumeTrue(JRE.majorVersion() == 8);
	}

	public static void xml(String expected, String actual) {
		Assert.assertEquals(trim(expected), trim(actual));
	}

	private static String trim(String input) {
		return Arrays.stream(input.split("\n"))
				.map(String::trim)
				.filter(str -> !str.isEmpty())
				.collect(Collectors.joining("\n"));
	}
}
