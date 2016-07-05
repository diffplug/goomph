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
package com.diffplug.gradle;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

public class LazyableTest {
	@Test
	public void doTest() {
		Lazyable<List<String>> lazyable = Lazyable.ofList();
		assertValue(lazyable);

		lazyable.getRoot().add("a");
		assertValue(lazyable, "a");
		lazyable.addLazyAction(list -> {
			list.add("1");
			list.add("2");
		});
		assertValue(lazyable, "a", "1", "2");

		lazyable.getRoot().add("b");
		assertValue(lazyable, "a", "b", "1", "2");
	}

	private void assertValue(Lazyable<List<String>> lazyable, String... result) {
		String actual = lazyable.getResult().stream().collect(Collectors.joining("\n"));
		String expected = Arrays.asList(result).stream().collect(Collectors.joining("\n"));
		Assert.assertEquals(expected, actual);
	}
}
