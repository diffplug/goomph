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
import com.diffplug.common.base.Throwing;
import com.diffplug.common.collect.ImmutableList;
import com.diffplug.common.collect.Lists;
import com.diffplug.common.swt.os.OS;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;

/**
 * Implementation of {@link CmdLineTask}, but helpful for implementing other things as well.
 */
public class CmdLine {
	private List<Throwing.Runnable> actions = Lists.newArrayList();

	private boolean echoCmd = true;
	private boolean echoOutput = true;

	/** Determines whether commands are echoed. Default value is true. */
	public void echoCmd(boolean echoCmd) {
		this.echoCmd = echoCmd;
	}

	/** Determines whether the output of commands are echoed. Default value is true. */
	public void echoOutput(boolean echoOutput) {
		this.echoOutput = echoOutput;
	}

	/** Executes the given command. */
	public void cmd(String cmd) {
		// use the project directory by default
		cmd(new File(System.getProperty("user.dir")), cmd);
	}

	/** Sets the working directory to the given dir, then executes the given command. */
	public void cmd(File workingDir, String cmd) {
		run(() -> {
			try {
				runCmd(workingDir, cmd, echoCmd, echoOutput);
			} catch (IOException e) {
				throw new RuntimeException(
						"cmd: " + cmd + "\n" +
								"dir: " + workingDir.getAbsolutePath(),
						e);
			}
		});
	}

	/** Removes the given file or directory. */
	public void rm(File fileOrDir) {
		run(() -> {
			if (fileOrDir.exists()) {
				FileMisc.forceDelete(fileOrDir);
			}
		});
	}

	/** Removes the given file or directory. */
	public void copy(File src, File dst) {
		run(() -> {
			if (!src.exists()) {
				throw new IllegalArgumentException("copy failed: " + src.getAbsolutePath() + " does not exist.");
			}

			if (src.isDirectory()) {
				FileUtils.copyDirectory(src, dst);
			} else {
				FileUtils.copyFile(src, dst);
			}
		});
	}

	/** Removes the given file or directory. */
	public void mv(File src, File dst) {
		run(() -> {
			if (!src.exists()) {
				throw new IllegalArgumentException("mv failed: " + src.getAbsolutePath() + " does not exist.");
			}

			if (src.isDirectory()) {
				FileUtils.moveDirectory(src, dst);
			} else {
				FileUtils.moveFile(src, dst);
			}
		});
	}

	/** Removes the given file or directory. */
	public void run(Throwing.Runnable action) {
		actions.add(action);
	}

	/** Runs the commands that have been queued up. */
	public void performActions() throws Throwable {
		for (Throwing.Runnable action : actions) {
			action.run();
		}
	}

	/** Runs the given command in the current working directory. */
	public static Result runCmd(String cmd) throws IOException {
		return runCmd(new File(System.getProperty("user.dir")), cmd);
	}

	/** Runs the given command in the given directory. */
	public static Result runCmd(File directory, String cmd) throws IOException {
		return runCmd(directory, cmd, true, true);
	}

	/** Runs the given command in the given directory with the given echo setting. */
	public static Result runCmd(File directory, String cmd, boolean echoCmd, boolean echoOutput) throws IOException {
		// set the cmds
		List<String> cmds = getPlatformCmds(cmd);
		ProcessBuilder builder = new ProcessBuilder(cmds);

		// set the working directory
		builder.directory(directory);

		// execute the command
		Process process = builder.start();

		// wrap the process' input and output
		try {
			if (echoCmd) {
				System.out.println("cmd>" + cmd);
			}

			InputStreamCollector stdInputThread = new InputStreamCollector(process.getInputStream(), echoOutput ? System.out : null);
			InputStreamCollector stdErrorThread = new InputStreamCollector(process.getErrorStream(), echoOutput ? System.err : null);

			// check that the process exited correctly
			int exitValue = process.waitFor();
			// then wait for threads collecting the output of thread
			stdInputThread.join();
			stdErrorThread.join();

			if (stdInputThread.getException() != null) {
				throw Errors.asRuntime(stdInputThread.getException());
			} else if (stdErrorThread.getException() != null) {
				throw Errors.asRuntime(stdErrorThread.getException());
			} else if (exitValue != EXIT_VALUE_SUCCESS) {
				throw new RuntimeException("'" + cmd + "' exited with " + exitValue);
			}

			// returns the result of this successful execution
			return new Result(directory, cmd, stdInputThread.getOutput(), stdErrorThread.getOutput());
		} catch (InterruptedException e) {
			// this isn't expected, but it's possible
			throw Errors.asRuntime(e);
		}
	}

	/** The integer value which marks that a process exited successfully. */
	private static final int EXIT_VALUE_SUCCESS = 0;

	/** Returns the given result. */
	public static class Result {
		public final File directory;
		public final String cmd;
		public final ImmutableList<String> output;
		public final ImmutableList<String> error;

		public Result(File directory, String cmd, ImmutableList<String> output, ImmutableList<String> error) {
			this.directory = directory;
			this.cmd = cmd;
			this.output = output;
			this.error = error;
		}
	}

	/** Prepends any arguments necessary to run a command. */
	private static List<String> getPlatformCmds(String cmd) {
		if (OS.getNative().isWindows()) {
			return Arrays.asList("cmd", "/c", cmd);
		} else {
			return Arrays.asList("/bin/sh", "-c", cmd);
		}
	}

	static class InputStreamCollector extends Thread {
		private final InputStream iStream;
		@Nullable
		private final PrintStream pStream;

		private final ImmutableList.Builder<String> output;

		private IOException exception;

		public InputStreamCollector(InputStream is, @Nullable PrintStream ps) {
			this.iStream = Objects.requireNonNull(is);
			this.pStream = ps;
			this.output = ImmutableList.builder();
			start();
		}

		@Override
		public synchronized void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(iStream, Charset.defaultCharset()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.add(line);
					if (pStream != null) {
						pStream.println(line);
					}
				}
			} catch (IOException ex) {
				this.exception = ex;
			}
		}

		public synchronized ImmutableList<String> getOutput() {
			return output.build();
		}

		public synchronized IOException getException() {
			return exception;
		}
	}
}
