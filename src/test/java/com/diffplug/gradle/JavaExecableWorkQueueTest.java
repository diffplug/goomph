/*
 * Copyright (C) 2016-2022 DiffPlug
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


import org.assertj.core.api.Assertions;
import org.junit.Test;

public class JavaExecableWorkQueueTest extends GradleIntegrationTest {
	@Test
	public void testInternal() throws Throwable {
		write("build.gradle",
				"plugins { id 'com.diffplug.eclipse.mavencentral' }",
				"import com.diffplug.gradle.JavaExecable",
				"import com.diffplug.gradle.JavaExecableTestIncrementer",
				"abstract class DemoTask extends DefaultTask {",
				"  @javax.inject.Inject",
				"  abstract public WorkerExecutor getWorkerExecutor()",
				"  @TaskAction",
				"  public void doIt() {",
				"    JavaExecableTestIncrementer value = new JavaExecableTestIncrementer(5)",
				"    JavaExecableTestIncrementer result = JavaExecable.exec(getWorkerExecutor().noIsolation(), value)",
				"    println('~~'  + result.output + '~~')",
				"  }",
				"}",
				"tasks.register('demo', DemoTask)");
		String output = gradleRunner().withGradleVersion("5.6").withArguments("demo").build().getOutput();
		Assertions.assertThat(output).contains("~~6~~");
	}
}
