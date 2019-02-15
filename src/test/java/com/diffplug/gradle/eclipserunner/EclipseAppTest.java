/*
 * Copyright 2019 DiffPlug
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

import org.junit.Assert;
import org.junit.Test;

import groovy.util.Node;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.StringPrinter;

public class EclipseAppTest {
	@Test
	public void test() {
		EclipseApp app = new EclipseApp("diffplug");
		app.addArg("prop", "a");
		app.addArg("prop", "b");
		app.addArg("flag");
		Assert.assertEquals("--launcher.suppressErrors -nosplash -application diffplug -prop a,b -flag", Joiner.on(" ").join(app.toArgList()));
		Assert.assertEquals("--launcher.suppressErrors\n-nosplash\n-application diffplug\n-prop a,b\n-flag\n", app.toString());
	}

	@Test
	public void testDoubleAdd() {
		EclipseApp app = new EclipseApp("diffplug");
		app.addArg("flag");
		app.addArg("flag");
		app.addArg("flag");
		Assert.assertEquals("--launcher.suppressErrors -nosplash -application diffplug -flag", Joiner.on(" ").join(app.toArgList()));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAnt() {
		EclipseApp.AntRunner ant = new EclipseApp.AntRunner();
		ant.define("key", "value");
		Node task = new Node(null, "anttask");
		task.attributes().put("prop", "propvalue");
		ant.setTask(task);

		Assert.assertEquals("--launcher.suppressErrors -nosplash -application org.eclipse.ant.core.antRunner -Dkey=value", Joiner.on(" ").join(ant.toArgList()));
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <anttask prop=\"propvalue\"/>",
				"</project>"), ant.buildXml());
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"### ARGS ###",
				"--launcher.suppressErrors",
				"-nosplash",
				"-application org.eclipse.ant.core.antRunner",
				"-Dkey=value",
				"",
				"### BUILD.XML ###",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><project>",
				"  <anttask prop=\"propvalue\"/>",
				"</project>"), ant.completeState());
	}
}
