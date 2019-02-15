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
package com.diffplug.gradle;

import java.io.File;

import com.diffplug.common.swt.os.OS;

/** Interface for the native file manager. */
public class NativeFileManager {
	/** Opens a file manager for the given file. */
	public static void open(File file) {
		try {
			String template = OS.getNative().winMacLinux("explorer /select,%s", "open %s", "xdg-open %s");
			String cmd = template.replace("%s", FileMisc.quote(file.getCanonicalFile()));
			CmdLine.runCmd(cmd);
		} catch (Exception e) {
			System.err.println("Unable to open folder in file manager: " + e.getMessage());
		}
	}
}
