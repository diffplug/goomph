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
package com.diffplug.gradle.oomph;


import com.diffplug.gradle.FileMisc;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WorkspaceRegistryTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void doTest() throws IOException {
		File registryFolder = folder.newFolder("registry");
		WorkspaceRegistry registry = new WorkspaceRegistry(registryFolder);

		// get a workspace dir for ideA
		File ideA = folder.newFolder("a");
		File workspaceA = registry.workspaceDir("a", ideA);

		// check the internals
		String hashA = "a-" + ideA.getAbsolutePath().hashCode();
		assertFolderContents(registryFolder,
				hashA + "/",
				hashA + "-owner");
		Assert.assertTrue(workspaceA.isDirectory());

		// get a workspace dir for ideB
		File ideB = folder.newFolder("b");
		File workspaceB = registry.workspaceDir("b", ideB);
		String hashB = "b-" + ideB.getAbsolutePath().hashCode();

		// check the internals
		assertFolderContents(registryFolder,
				hashA + "/",
				hashA + "-owner",
				hashB + "/",
				hashB + "-owner");
		Assert.assertTrue(workspaceB.isDirectory());

		// do a clean
		registry.clean();

		// no change
		assertFolderContents(registryFolder,
				hashA + "/",
				hashA + "-owner",
				hashB + "/",
				hashB + "-owner");
		Assert.assertTrue(workspaceB.isDirectory());

		// remove ideB and clean, its workspace should go away
		FileMisc.forceDelete(ideB);
		registry.clean();
		assertFolderContents(registryFolder,
				hashA + "/",
				hashA + "-owner");
		Assert.assertTrue(workspaceA.isDirectory());

		// create a new registry, it should have the same behavior
		registry = new WorkspaceRegistry(registryFolder);
		registry.clean();
		assertFolderContents(registryFolder,
				hashA + "/",
				hashA + "-owner");

		// remove ideA and clean
		FileMisc.forceDelete(ideA);
		registry.clean();
		assertFolderContents(registryFolder);
	}

	static void assertFolderContents(File folder, String... contents) {
		String actual = FileMisc.list(folder).stream().map(file -> file.getName() + (file.isFile() ? "" : "/")).sorted().collect(Collectors.joining("\n"));
		String expected = Arrays.asList(contents).stream().sorted().collect(Collectors.joining("\n"));
		Assert.assertEquals(expected, actual);
	}
}
