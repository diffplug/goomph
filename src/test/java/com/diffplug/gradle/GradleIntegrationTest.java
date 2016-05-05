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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class GradleIntegrationTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected void write(String path, String content) throws IOException {
		File file = new File(folder.getRoot(), path);
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
	}

	protected String read(String path) throws IOException {
		File file = new File(folder.getRoot(), path);
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}

	protected GradleRunner gradleRunner() {
		return GradleRunner.create().withProjectDir(folder.getRoot()).withPluginClasspath();
	}
}
