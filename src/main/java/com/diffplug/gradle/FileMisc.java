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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.collect.Maps;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.OS;

/** Miscellaneous utilties for copying files around. */
public class FileMisc {
	///////////////////////////////////////////////////////////////////
	// Replacements for File.* which check exceptional return values //
	///////////////////////////////////////////////////////////////////
	/** Lists the children of the given file in a safe way ({@link File#listFiles()} can return null). */
	public static List<File> list(File d) {
		return retry(d, dir -> {
			File[] children = dir.listFiles();
			if (children == null) {
				if (dir.isFile()) {
					throw new IllegalArgumentException("Can't list " + dir + " because it is a file, not a directory.");
				} else if (!dir.exists()) {
					throw new IllegalArgumentException("Can't list " + dir + " because it does not exist.");
				} else {
					throw new IllegalArgumentException("Can't list " + dir + ", not sure why.");
				}
			} else {
				return Arrays.asList(children);
			}
		});
	}

	/** Calls {@link File#mkdirs()} and throws an exception if it fails. */
	public static void mkdirs(File d) {
		retry(d, dir -> {
			java.nio.file.Files.createDirectories(dir.toPath());
			return null;
		});
	}

	/** Calls {@link FileUtils#forceDelete(File)} and throws an exception if it fails.  If the file doesn't exist at all, that's fine. */
	public static void forceDelete(File f) {
		retry(f, file -> {
			if (file.exists()) {
				FileUtils.forceDelete(f);
			}
			return null;
		});
	}

	private static final int MS_RETRY = 500;

	/**
	 * Retries an action every ms, for 250ms, until it finally works or fails. 
	 *
	 * Makes FS operations more reliable.
	 */
	private static <T> T retry(File input, Throwing.Function<File, T> function) {
		long start = System.currentTimeMillis();
		Throwable lastException;
		do {
			try {
				return function.apply(input);
			} catch (Throwable e) {
				lastException = e;
				Errors.suppress().run(() -> Thread.sleep(1));
			}
		} while (System.currentTimeMillis() - start < MS_RETRY);
		throw Errors.asRuntime(lastException);
	}

	//////////////////////////////
	// Misc string manipulation //
	//////////////////////////////
	/** Enforces unix newlines on the given string. */
	public static String toUnixNewline(String input) {
		return input.replace("\r\n", "\n");
	}

	/** Quotes the given input string iff it contains whitespace. */
	public static String quote(String input) {
		if (input.contains(" ")) {
			return "\"" + input + "\"";
		} else {
			return input;
		}
	}

	/** Quotes the absolute path of the given file iff it contains whitespace. */
	public static String quote(File input) {
		return quote(input.getAbsolutePath());
	}

	/** Throws an exception if the given input property contains whitespace. */
	public static String noQuote(String input) {
		if (input.contains(" ")) {
			throw new IllegalArgumentException("Cannot contain whitespace: '" + input + "'");
		} else {
			return input;
		}
	}

	/////////////////////////////////////
	// Quick-n-dirty directory markers //
	/////////////////////////////////////
	/** Writes a file with the given name, to the given directory, containing the given value. */
	public static void writeToken(File dir, String name, String value) throws IOException {
		Preconditions.checkArgument(dir.isDirectory(), "Need to create directory first!  %s", dir);
		File token = new File(dir, name);
		FileUtils.write(token, value, StandardCharsets.UTF_8);
	}

	/** Writes a token file containing the given value. */
	public static void writeTokenFile(File tokenFile, String value) throws IOException {
		writeToken(tokenFile.getParentFile(), tokenFile.getName(), value);
	}

	/** Returns the contents of a file with the given name, if it exists. */
	public static Optional<String> readToken(File dir, String name) throws IOException {
		File token = new File(dir, name);
		if (!token.isFile()) {
			return Optional.empty();
		} else {
			return Optional.of(FileUtils.readFileToString(token, StandardCharsets.UTF_8));
		}
	}

	/** Writes an empty file with the given name in the given directory. */
	public static void writeToken(File dir, String name) throws IOException {
		writeToken(dir, name, "");
	}

	/** Returns true iff the given directory has a file with the given name. */
	public static boolean hasToken(File dir, String name) throws IOException {
		return readToken(dir, name).isPresent();
	}

	/** Returns true iff the given directory has a file with the given name containing the given content. */
	public static boolean hasToken(File dir, String name, String content) throws IOException {
		return readToken(dir, name).map(str -> content.equals(str)).orElse(false);
	}

	/** Returns true iff the given directory has a file with the given name. */
	public static boolean hasTokenFile(File tokenFile) throws IOException {
		return hasToken(tokenFile.getParentFile(), tokenFile.getName());
	}

	/** Returns true iff the given directory has a file with the given name containing the given content. */
	public static boolean hasTokenFile(File tokenFile, String content) throws IOException {
		return hasToken(tokenFile.getParentFile(), tokenFile.getName(), content);
	}

	////////////////////////////
	// Misc file manipulation //
	////////////////////////////
	/**
	 * Copies from src to dst and performs a simple
	 * copy-replace templating operation along the way.
	 * 
	 * ```java
	 * copyFile(src, dst,
	 *     "%username%", "lskywalker"
	 *     "%firstname%", "Luke",
	 *     "%lastname%", "Skywalker");
	 * ```
	 */
	public static void copyFile(File srcFile, File dstFile, String... toReplace) throws IOException {
		// make a map of the keys that we're replacing
		Preconditions.checkArgument(toReplace.length % 2 == 0);
		Map<String, String> replaceMap = Maps.newHashMap();
		for (int i = 0; i < toReplace.length / 2; ++i) {
			replaceMap.put(toReplace[2 * i], toReplace[2 * i + 1]);
		}
		// replace them
		String content = Joiner.on("\n").join(Files.readLines(srcFile, StandardCharsets.UTF_8));
		for (Entry<String, String> entry : replaceMap.entrySet()) {
			content = content.replace(entry.getKey(), entry.getValue());
		}
		// write it out
		mkdirs(dstFile.getParentFile());
		Files.write(content.getBytes(StandardCharsets.UTF_8), dstFile);
	}

	/** Modifies the given file in place. */
	public static void modifyFile(File file, Function<String, String> modifier) throws IOException {
		String content = new String(Files.toByteArray(file), StandardCharsets.UTF_8);
		String result = modifier.apply(content);
		Files.write(result.getBytes(StandardCharsets.UTF_8), file);
	}

	/** Deletes the given file or directory if it exists, then creates a fresh directory in its place. */
	public static void cleanDir(File dirToRemove) throws IOException {
		if (dirToRemove.isFile()) {
			FileMisc.forceDelete(dirToRemove);
		} else if (dirToRemove.isDirectory()) {
			try {
				FileUtils.deleteDirectory(dirToRemove);
			} catch (IOException e) {
				// we couldn't delete the directory,
				// but deleting everything inside is just as good
				for (File file : FileMisc.list(dirToRemove)) {
					FileMisc.forceDelete(file);
				}
			}
		}
		mkdirs(dirToRemove);
	}

	/**
	 * Flattens a single directory (moves its children to be its peers, then deletes the given directory.
	 * 
	 * ```
	 * before:
	 *     root/
	 *        toFlatten/
	 *           child1
	 *           child2
	 * 
	 * flatten("root/toFlatten")
	 * 
	 * after:
	 *     root/
	 *        child1
	 *        child2
	 * ```
	 */
	public static void flatten(File dirToRemove) throws IOException {
		final File parent = dirToRemove.getParentFile();
		// move each child directory to the parent
		for (File child : FileMisc.list(dirToRemove)) {
			boolean createDestDir = false;
			if (child.isFile()) {
				FileUtils.moveFileToDirectory(child, parent, createDestDir);
			} else if (child.isDirectory()) {
				FileUtils.moveDirectoryToDirectory(child, parent, createDestDir);
			} else {
				throw new IllegalArgumentException("Unknown filetype: " + child);
			}
		}
		// remove the directory which we're flattening away
		FileMisc.forceDelete(dirToRemove);
	}

	/** Concats the first files and writes them to the last file. */
	public static void concat(Iterable<File> toMerge, File dst) throws IOException {
		try (FileChannel dstChannel = FileChannel.open(dst.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			for (File file : toMerge) {
				try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
					FileChannel channel = raf.getChannel();
					dstChannel.write(channel.map(FileChannel.MapMode.READ_ONLY, 0, raf.length()));
				}
			}
		}
	}

	public static List<File> parseListFile(Project project, List<Object> inputs) {
		return inputs.stream().map(project::file).collect(Collectors.toList());
	}

	///////////////////////////
	// Unix file permissions //
	///////////////////////////
	/** Permission bits. */
	private static final int OWNER_READ_FILEMODE = 0400;
	private static final int OWNER_WRITE_FILEMODE = 0200;
	private static final int OWNER_EXEC_FILEMODE = 0100;
	private static final int GROUP_READ_FILEMODE = 0040;
	private static final int GROUP_WRITE_FILEMODE = 0020;
	private static final int GROUP_EXEC_FILEMODE = 0010;
	private static final int OTHERS_READ_FILEMODE = 0004;
	private static final int OTHERS_WRITE_FILEMODE = 0002;
	private static final int OTHERS_EXEC_FILEMODE = 0001;

	/** Converts a set of {@link PosixFilePermission} to chmod-style octal file mode. */
	public static int toOctalFileModeInt(Set<PosixFilePermission> permissions) {
		int result = 0;
		for (PosixFilePermission permissionBit : permissions) {
			switch (permissionBit) {
			case OWNER_READ:
				result |= OWNER_READ_FILEMODE;
				break;
			case OWNER_WRITE:
				result |= OWNER_WRITE_FILEMODE;
				break;
			case OWNER_EXECUTE:
				result |= OWNER_EXEC_FILEMODE;
				break;
			case GROUP_READ:
				result |= GROUP_READ_FILEMODE;
				break;
			case GROUP_WRITE:
				result |= GROUP_WRITE_FILEMODE;
				break;
			case GROUP_EXECUTE:
				result |= GROUP_EXEC_FILEMODE;
				break;
			case OTHERS_READ:
				result |= OTHERS_READ_FILEMODE;
				break;
			case OTHERS_WRITE:
				result |= OTHERS_WRITE_FILEMODE;
				break;
			case OTHERS_EXECUTE:
				result |= OTHERS_EXEC_FILEMODE;
				break;
			}
		}
		return result;
	}

	/** Converts a set of {@link PosixFilePermission} to chmod-style octal file mode. */
	public static String toOctalFileMode(Set<PosixFilePermission> permissions) {
		int value = toOctalFileModeInt(permissions);
		return Integer.toOctalString(value);
	}

	/** Returns true if any of the bits contain the executable permission. */
	public static boolean containsExecutablePermission(Set<PosixFilePermission> permissions) {
		return permissions.contains(PosixFilePermission.OWNER_EXECUTE) &&
				permissions.contains(PosixFilePermission.GROUP_EXECUTE) &&
				permissions.contains(PosixFilePermission.OTHERS_EXECUTE);
	}

	/** The `file://` protocol. */
	public static final String PROTOCOL = "file://";

	/** Prefixes `file://` to the file's absolute path. */
	public static String asUrl(File file) {
		return PROTOCOL + file.getAbsolutePath();
	}

	/** Returns ".app" on macOS, and empty string on all others. */
	public static String macApp() {
		return OS.getNative().winMacLinux("", ".app", "");
	}

	/** Returns "Contents/Eclipse/" on macOS, and empty string on all others. */
	public static String macContentsEclipse() {
		return OS.getNative().winMacLinux("", "Contents/Eclipse/", "");
	}

	/** Ensures that the given file ends with ".app" on macOS, does nothing on all others. */
	public static void assertMacApp(File file) {
		if (OS.getNative().isMac()) {
			Preconditions.checkArgument(file.getName().endsWith(".app"), "Mac installations must end in .app");
		}
	}
}
