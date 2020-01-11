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
package com.diffplug.gradle.p2;


import com.diffplug.gradle.eclipserunner.EquinoxLauncher;
import com.diffplug.gradle.osgi.OsgiExecable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class OsgiExecableTest {
	static class Incrementer implements OsgiExecable {
		private static final long serialVersionUID = -5728572785844814830L;

		int input;
		int output;

		Incrementer(int input) {
			this.input = input;
		}

		public int getOutput() {
			return output;
		}

		@Override
		public void run() {
			output = input + 1;
		}
	}

	@Test
	public void testInternal() throws Throwable {
		// obvious
		Incrementer example = new Incrementer(5);
		example.run();
		Assert.assertEquals(6, example.output);
	}

	@Test
	@Ignore // This works in Eclipse, works in real-life, but fails in gradle's test runner.  Dunno why.
	public void testExternal() throws Throwable {
		// magic
		P2BootstrapInstallation installation = P2BootstrapInstallation.latest();
		installation.ensureInstalled();

		EquinoxLauncher launcher = new EquinoxLauncher(installation.getRootFolder());
		try (EquinoxLauncher.Running running = launcher.open()) {
			Incrementer example = new Incrementer(5);
			Incrementer result = OsgiExecable.exec(running.bundleContext(), example);
			Assert.assertEquals(6, result.output);
		}
	}
}
