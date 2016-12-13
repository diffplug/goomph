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

import java.io.IOException;
import java.io.Writer;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;

public class GoomphCacheLocationInitFromProjectTest extends GradleIntegrationTest {
	@Test
	public void ensureOverrideWorks() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.oomph.ide'",
				"}",
				"ext.goomph_override_p2bootstrapUrl='somewhere'",
				"com.diffplug.gradle.GoomphCacheLocations.initFromProject(project)",
				"System.out.println(com.diffplug.gradle.GoomphCacheLocations.p2bootstrapUrl())",
				// if we leave it, it will muck with future testkit tests
				"com.diffplug.gradle.GoomphCacheLocations.override_p2bootstrapUrl=null");
		StringBuilder buffer = new StringBuilder();
		StringPrinter printer = new StringPrinter(buffer::append);
		try (Writer writer = printer.toPrintWriter()) {
			gradleRunner().forwardStdOutput(writer).build();
		}
		String firstLine = buffer.toString().split("\n")[0];
		Assert.assertEquals("Optional[somewhere]", firstLine);
	}
}
