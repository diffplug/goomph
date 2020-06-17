/*
 * Copyright (C) 2016-2019 DiffPlug
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


import java.io.File;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileMiscTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testDeleteEmptyFolder() throws IOException {
		File a = folder.newFolder("a");
		File b = folder.newFolder("b");
		File b1 = new File(b, "1");
		b1.createNewFile();

		FileMisc.deleteEmptyFolders(folder.getRoot());
		Assert.assertEquals(false, a.exists());
		Assert.assertEquals(true, b.exists());
		Assert.assertEquals(true, b1.exists());
	}

	@Test
	public void testDeleteEmptyFolderRecursive() throws IOException {
		File a = folder.newFolder("a");
		File a1 = new File(a, "1");
		a1.createNewFile();
		File b = folder.newFolder("b");
		File b1 = new File(b, "1");
		b1.mkdir();
		File b2 = new File(b, "2");
		b2.mkdir();
		File b21 = new File(b2, "1");
		b21.mkdir();

		FileMisc.deleteEmptyFolders(folder.getRoot());
		Assert.assertEquals(true, a.exists());
		Assert.assertEquals(true, a1.exists());
		Assert.assertEquals(false, b.exists());
		Assert.assertEquals(false, b1.exists());
		Assert.assertEquals(false, b2.exists());
		Assert.assertEquals(false, b21.exists());
	}

	@Test
	public void testDeleteEmptyFolderRecursiveStopsAtRoot() throws IOException {
		File a = folder.newFolder("a");
		File b = folder.newFolder("b");

		FileMisc.deleteEmptyFolders(folder.getRoot());
		Assert.assertEquals(false, a.exists());
		Assert.assertEquals(false, b.exists());
		Assert.assertEquals(true, folder.getRoot().exists());
	}
}
