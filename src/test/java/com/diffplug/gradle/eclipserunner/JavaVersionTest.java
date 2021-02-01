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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class JavaVersionTest {

	@Test
	public void parse() {
		JavaVersion v = JavaVersion.fromString("1.8.0_171");
		Assert.assertEquals(1, v.getMajor());
		Assert.assertEquals(8, v.getMinor());
		Assert.assertEquals(0, v.getFix());
		v = JavaVersion.fromString("1.9.0");
		Assert.assertEquals(1, v.getMajor());
		Assert.assertEquals(9, v.getMinor());
		Assert.assertEquals(0, v.getFix());
		v = JavaVersion.fromString("11.0.8");
		Assert.assertEquals(11, v.getMajor());
		Assert.assertEquals(0, v.getMinor());
		Assert.assertEquals(8, v.getFix());
	}

	@Test
	public void goe() {
		JavaVersion v = JavaVersion.fromString("1.8.0");
		assertFalse(v.isGreaterOrEqual("1.9.0"));
		assertFalse(v.isGreaterOrEqual("1.8.10"));
		assertFalse(v.isGreaterOrEqual("11.0.1"));

		assertTrue(v.isGreaterOrEqual("1.7.9"));
		assertTrue(v.isGreaterOrEqual("1.5.0"));
		assertTrue(v.isGreaterOrEqual("1.7.9"));

		v = JavaVersion.fromString("11.0.0");
		assertFalse(v.isGreaterOrEqual("13.0.0"));
		assertFalse(v.isGreaterOrEqual("14.0.1"));
		assertFalse(v.isGreaterOrEqual("11.0.1"));

		assertTrue(v.isGreaterOrEqual("1.8.0_171"));
		assertTrue(v.isGreaterOrEqual("1.5.0"));
		assertTrue(v.isGreaterOrEqual("1.7.9"));
	}
}
