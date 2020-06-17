/*
 * Copyright (C) 2016-2020 DiffPlug
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


import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;

public class GoomphCacheLocationInitFromProjectTest extends GradleIntegrationTest {
	@Test
	public void ensureOverrideWorks() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.oomph.ide'",
				"}",
				"ext.goomph_override_p2bootstrapUrl='somewhere'",
				"com.diffplug.gradle.GoomphCacheLocations.initFromProject(project)",
				"System.out.println(\"test\");",
				"System.out.println(\"p2bootstrapUrl=\" + com.diffplug.gradle.GoomphCacheLocations.p2bootstrapUrl())",
				// if we leave it, it will muck with future testkit tests
				"project.afterEvaluate { com.diffplug.gradle.GoomphCacheLocations.override_p2bootstrapUrl=null }");
		BuildResult build = gradleRunner().build();
		Assertions.assertThat(build.getOutput()).contains("p2bootstrapUrl=Optional[somewhere]");
	}
}
