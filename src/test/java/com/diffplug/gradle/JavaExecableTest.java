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

import org.junit.Assert;
import org.junit.Test;

public class JavaExecableTest {
	static class Incrementer implements JavaExecable {
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
		public void run() throws Throwable {
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
	public void testExternal() throws Throwable {
		// magic
		Incrementer example = new Incrementer(5);
		Incrementer result = JavaExecable.execWithoutGradle(example);
		Assert.assertEquals(6, result.output);
	}

	static class Thrower implements JavaExecable {
		private static final long serialVersionUID = -852084000617220161L;

		@Override
		public void run() throws Throwable {
			throw new SpecialException();
		}
	}

	static class SpecialException extends Exception {
		private static final long serialVersionUID = -1396270428919593918L;
	}

	@Test(expected = SpecialException.class)
	public void testThrowerInternal() throws Throwable {
		Thrower example = new Thrower();
		example.run();
	}

	@Test(expected = SpecialException.class)
	public void testThrowerExternal() throws Throwable {
		Thrower example = new Thrower();
		JavaExecable.execWithoutGradle(example);
	}
}
