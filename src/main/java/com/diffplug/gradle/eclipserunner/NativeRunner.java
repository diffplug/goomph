/*
 * Copyright (C) 2015-2019 DiffPlug
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
package com.diffplug.gradle.eclipserunner;


import com.diffplug.gradle.CmdLine;
import com.diffplug.gradle.FileMisc;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Runs an `EclipseApp` using a native launcher (such as {@code eclipsec.exe}). 
 */
public class NativeRunner implements EclipseRunner {
	final File eclipsec;

	/** Pass it the location of the launcher file. */
	public NativeRunner(File eclipsec) {
		this.eclipsec = Objects.requireNonNull(eclipsec);
	}

	@Override
	public void run(List<String> args) throws Exception {
		StringBuilder builder = new StringBuilder();
		// add eclipsec
		builder.append(FileMisc.quote(eclipsec));
		for (String arg : args) {
			// space
			builder.append(' ');
			// arg (possibly quoted)
			builder.append(FileMisc.quote(arg));
		}
		// execute the cmd
		CmdLine.runCmd(builder.toString());
	}
}
