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

import com.diffplug.common.base.StringPrinter;

import groovy.util.Node;

public class EclipseAppTest {
	@Test
	public void test() {
		EclipseApp app = new EclipseApp("diffplug");
		app.addArg("prop", "a");
		app.addArg("prop", "b");
		app.addArg("flag");
		Assert.assertEquals("-application diffplug -prop a,b -flag", Joiner.on(" ").join(app.toArgList()));
		Assert.assertEquals("-application diffplug\n-prop a,b\n-flag\n", app.toString());
	}

	@Test
	public void testDoubleAdd() {
		EclipseApp app = new EclipseApp("diffplug");
		app.addArg("flag");
		app.addArg("flag");
		app.addArg("flag");
		Assert.assertEquals("-application diffplug -flag", Joiner.on(" ").join(app.toArgList()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAnt() {
		EclipseApp.Ant ant = new EclipseApp.Ant();
		ant.define("key", "value");
		Node task = new Node(null, "anttask");
		task.attributes().put("prop", "propvalue");
		ant.setTask(task);

		Assert.assertEquals("-application org.eclipse.ant.core.antRunner -Dkey=value", Joiner.on(" ").join(ant.toArgList()));
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <anttask prop=\"propvalue\"/>",
				"</project>"), ant.buildXml());
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"-application org.eclipse.ant.core.antRunner",
				"-Dkey=value",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <anttask prop=\"propvalue\"/>",
				"</project>"), ant.completeState());
	}
}
