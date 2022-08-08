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
package com.diffplug.gradle;


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.io.ByteSink;
import com.diffplug.common.io.ByteSource;
import com.diffplug.common.io.ByteStreams;
import com.diffplug.common.io.Files;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

/** Utilities for mucking with zip files. */
public class ZipMisc {
	/**
	 * Reads the given entry from the zip.
	 * 
	 * @param input		a zip file
	 * @param toRead	a path within that zip file
	 * @param reader	will be called with an InputStream containing the contents of that entry in the zip file
	 */
	public static void read(File input, String toRead, Throwing.Specific.Consumer<InputStream, IOException> reader) throws IOException {
		try (
				ZipFile file = new ZipFile(input);
				InputStream stream = file.getInputStream(file.getEntry(toRead));) {
			reader.accept(stream);
		} catch (NullPointerException e) {
			if (e.getMessage().equals("entry")) {
				System.err.println("No such entry: " + toRead);
				try (ZipFile file = new ZipFile(input)) {
					Enumeration<? extends ZipEntry> entries = file.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						System.err.println("  available: " + entry.getName());
					}
				}
			}
		}
	}

	/**
	 * Reads the given entry from the zip.
	 * 
	 * @param input		a zip file
	 * @param toRead	a path within that zip file
	 * @return the given path within the zip file decoded as a UTF8 string, with only unix newlines.
	 */
	public static String read(File input, String toRead) throws IOException {
		String raw = StringPrinter.buildString(Errors.rethrow().wrap(printer -> {
			read(input, toRead, inputStream -> {
				copy(inputStream, printer.toOutputStream(StandardCharsets.UTF_8));
			});
		}));
		return FileMisc.toUnixNewline(raw);
	}

	/**
	 * Modifies only the specified entries in a zip file. 
	 *
	 * @param input 		a source from a zip file
	 * @param output		an output to a zip file
	 * @param toModify		a map from path to an input stream for the entries you'd like to change
	 * @param toOmit		a set of entries you'd like to leave out of the zip
	 * @throws IOException
	 */
	public static void modify(ByteSource input, ByteSink output, Map<String, Function<byte[], byte[]>> toModify, Predicate<String> toOmit) throws IOException {
		try (ZipInputStream zipInput = new ZipInputStream(input.openBufferedStream());
				ZipOutputStream zipOutput = new ZipOutputStream(output.openBufferedStream())) {
			while (true) {
				// read the next entry
				ZipEntry entry = zipInput.getNextEntry();
				if (entry == null) {
					break;
				}

				Function<byte[], byte[]> replacement = toModify.get(entry.getName());
				if (replacement != null) {
					byte[] clean = ByteStreams.toByteArray(zipInput);
					byte[] modified = replacement.apply(clean);
					// if it's the entry being modified, enter the modified stuff
					try (InputStream replacementStream = new ByteArrayInputStream(modified)) {
						ZipEntry newEntry = new ZipEntry(entry.getName());
						newEntry.setComment(entry.getComment());
						newEntry.setExtra(entry.getExtra());
						newEntry.setMethod(entry.getMethod());
						newEntry.setTime(entry.getTime());

						zipOutput.putNextEntry(newEntry);
						copy(replacementStream, zipOutput);
					}
				} else if (!toOmit.test(entry.getName())) {
					// if it isn't being modified, just copy the file stream straight-up
					ZipEntry newEntry = new ZipEntry(entry);
					newEntry.setCompressedSize(-1);
					zipOutput.putNextEntry(newEntry);
					copy(zipInput, zipOutput);
				}

				// close the entries
				zipInput.closeEntry();
				zipOutput.closeEntry();
			}
		}
	}

	/** Modifies a file in-place. */
	public static void modify(File file, Map<String, Function<byte[], byte[]>> toModify, Predicate<String> toOmit) throws IOException {
		byte[] allContent = Files.asByteSource(file).read();
		ByteSource source = ByteSource.wrap(allContent);
		ByteSink sink = Files.asByteSink(file);
		modify(source, sink, toModify, toOmit);
	}

	/**
	 * Creates a single-entry zip file.
	 * 
	 * @param input					an uncompressed file
	 * @param pathWithinArchive		the path within the archive
	 * @param output				the new zip file it will be compressed into
	 */
	public static void zip(File input, String pathWithinArchive, File output) throws IOException {
		try (ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
			zipStream.setMethod(ZipOutputStream.DEFLATED);
			zipStream.setLevel(9);
			zipStream.putNextEntry(new ZipEntry(pathWithinArchive));
			try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(input))) {
				copy(inputStream, zipStream);
			}
		}
	}

	/** Copies one stream into the other. */
	private static void copy(InputStream input, OutputStream output) throws IOException {
		IOUtils.copy(input, output);
	}

	/**
	 * Unzips a directory to a folder.
	 *
	 * @param input				a zip file
	 * @param destinationDir	where the zip will be extracted to
	 */
	public static void unzip(File input, File destinationDir) throws IOException {
		try (ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(input)))) {
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				File dest = new File(destinationDir, entry.getName());
				if (!dest.toPath().normalize().startsWith(destinationDir.toPath().normalize())) {
					throw new RuntimeException("Bad zip entry");
				}
				if (entry.isDirectory()) {
					FileMisc.mkdirs(dest);
				} else {
					FileMisc.mkdirs(dest.getParentFile());
					try (OutputStream output = new BufferedOutputStream(new FileOutputStream(dest))) {
						copy(zipInput, output);
					}
				}
			}
		}
	}
}
