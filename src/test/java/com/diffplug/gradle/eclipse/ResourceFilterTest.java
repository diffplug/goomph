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

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.gradle.eclipse.ResourceFilter.Kind;

public class ResourceFilterTest {
	@Test
	public void testKindFlag() {
		Assert.assertEquals(1, Kind.INCLUDE_ONLY.flag());
		Assert.assertEquals(2, Kind.EXCLUDE_ALL.flag());
		Assert.assertEquals(4, Kind.FILES.flag());
		Assert.assertEquals(8, Kind.FOLDERS.flag());
		Assert.assertEquals(16, Kind.INHERITABLE.flag());
	}

	@Test
	public void testKindCreate() {
		Assert.assertEquals(5, Kind.type(Kind.INCLUDE_ONLY, Kind.FILES));
	}
}
