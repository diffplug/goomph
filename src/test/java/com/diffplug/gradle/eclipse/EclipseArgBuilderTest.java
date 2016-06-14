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

import org.gradle.internal.impldep.com.google.common.base.Joiner;
import org.junit.Assert;
import org.junit.Test;

public class EclipseArgBuilderTest {
	@Test
	public void test() {
		EclipseArgsBuilder builder = new EclipseArgsBuilder();
		builder.addArg("prop", "a");
		builder.addArg("prop", "b");
		builder.addArg("flag");
		Assert.assertEquals("-prop a,b -flag", Joiner.on(" ").join(builder.toArgList()));
		Assert.assertEquals("-prop a,b\n-flag\n", builder.toString());
	}
}
