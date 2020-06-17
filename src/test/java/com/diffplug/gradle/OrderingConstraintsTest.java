/*
 * Copyright (C) 2016-2019 DiffPlug
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


import com.diffplug.common.base.StringPrinter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

public class OrderingConstraintsTest {
	@Test
	public void testRequiresUnsatisfied() {
		UnderTest list = new UnderTest();
		list.add("cake").require("batter");
		list.assertException("cake requires batter, but it is not present.");
	}

	@Test
	public void testRequiresSatisfied() {
		UnderTest list = new UnderTest();
		list.add("cake").require("batter");
		list.add("batter");
		list.assertValues("cake", "batter");
	}

	@Test
	public void testBefore() {
		UnderTest list = new UnderTest();
		list.add("cake");
		list.add("batter").before("cake");
		list.assertValues("batter", "cake");
	}

	@Test
	public void testAfter() {
		UnderTest list = new UnderTest();
		list.add("batter");
		list.add("cake").after("batter");
		list.assertValues("batter", "cake");
	}

	@Test
	public void testBeforeAfterUnsatisfied() {
		UnderTest list = new UnderTest();
		list.add("batter").after("cake");
		list.add("cake").after("batter");
		list.assertException(StringPrinter.buildStringFromLines(
				"Could not satisfy order constraints:",
				"batter must be after cake",
				"cake must be after batter"));
	}

	static class UnderTest {
		List<String> list = new ArrayList<>();
		Map<String, OrderingConstraints<String>> constraints = new HashMap<>();

		public OrderingConstraints<String> add(String value) {
			list.add(value);
			OrderingConstraints<String> result = new OrderingConstraints<>();
			constraints.put(value, result);
			return result;
		}

		public void assertException(String message) {
			try {
				OrderingConstraints.satisfy(list, constraints::get);
				Assert.fail();
			} catch (Exception e) {
				Assert.assertEquals(e.getMessage(), message);
			}
		}

		public void assertValues(String... values) {
			String expected = Arrays.asList(values).stream().collect(Collectors.joining("\n"));
			String actual = OrderingConstraints.satisfy(list, constraints::get).stream().collect(Collectors.joining("\n"));
			Assert.assertEquals(expected, actual);
		}
	}
}
