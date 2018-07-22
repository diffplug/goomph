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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.Diff;
import com.diffplug.gradle.GradleIntegrationTest;

public class ExcludeBuildFolderPluginTest extends GradleIntegrationTest {
	@Test
	public void assertProjectChanged() throws IOException {
		// write the normal eclipse file
		String plainEclipse = testCase("eclipse");
		// write the excluded build folder file
		String underTestEclipse = testCase("com.diffplug.gradle.eclipse.excludebuildfolder");
		// assert the expected thing was added to the .project file
		Assert.assertEquals(StringPrinter.buildStringFromLines(
				"DELETE",
				"/",
				"INSERT",
				">",
				"		<filter>",
				"			<id>somenumber</id>",
				"			<name></name>",
				"			<type>10</type>",
				"			<matcher>",
				"				<id>org.eclipse.ui.ide.multiFilter</id>",
				"				<arguments>1.0-name-matches-false-false-build</arguments>",
				"			</matcher>",
				"		</filter>",
				"	</filteredResources"), Diff.computeDiff(plainEclipse, underTestEclipse));
	}

	private String testCase(String pluginId) throws IOException {
		write("build.gradle", "plugins { id '" + pluginId + "' }");
		gradleRunner().withArguments("eclipse").build();
		return read(".project").replaceAll("<id>([0-9|-]+)</id>", "<id>somenumber</id>");
	}
}
