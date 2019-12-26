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
package com.diffplug.gradle;


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class GradleIntegrationTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected void write(String path, String... lines) throws IOException {
		String content = Arrays.asList(lines).stream().collect(Collectors.joining("\n")) + "\n";
		Path target = folder.getRoot().toPath().resolve(path);
		Files.createDirectories(target.getParent());
		Files.write(target, content.getBytes(StandardCharsets.UTF_8));
	}

	protected String read(String path) throws IOException {
		Path target = folder.getRoot().toPath().resolve(path);
		String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
		return FileMisc.toUnixNewline(content);
	}

	protected File file(String path) {
		return folder.getRoot().toPath().resolve(path).toFile();
	}

	protected GradleRunner gradleRunner() {
		return GradleRunner.create().withProjectDir(folder.getRoot()).withPluginClasspath();
	}

	/** Dumps the complete file contents of the folder to the console. */
	protected String getContents() throws IOException {
		return getContents(subPath -> !subPath.startsWith(".gradle"));
	}

	protected String getContents(Predicate<String> subpathsToInclude) throws IOException {
		TreeDef<File> treeDef = TreeDef.forFile(Errors.rethrow());
		List<File> files = TreeStream.depthFirst(treeDef, folder.getRoot())
				.filter(file -> file.isFile())
				.collect(Collectors.toList());

		ListIterator<File> iterator = files.listIterator(files.size());
		int rootLength = folder.getRoot().getAbsolutePath().length() + 1;
		return StringPrinter.buildString(printer -> {
			Errors.rethrow().run(() -> {
				while (iterator.hasPrevious()) {
					File file = iterator.previous();
					String subPath = file.getAbsolutePath().substring(rootLength);
					if (subpathsToInclude.test(subPath)) {
						printer.println("### " + subPath + " ###");
						printer.println(read(subPath));
					}
				}
			});
		});
	}

	/** Copies the test to some directory for external debugging. */
	protected void copyTo(String path) throws IOException {
		File destination = new File(path);
		FileUtils.copyDirectory(folder.getRoot(), destination);
	}
}
