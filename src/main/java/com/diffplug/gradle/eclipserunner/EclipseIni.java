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
package com.diffplug.gradle.eclipserunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.gradle.internal.impldep.com.google.common.base.Preconditions;

import com.diffplug.common.base.Unhandled;

/**
 * Api for manipulating eclipse.ini, see
 * [Eclipse docs](https://wiki.eclipse.org/Eclipse.ini)
 * for more details.
 */
public class EclipseIni {
	/** Models double, single, and no dash. */
	private enum Dash {
		DOUBLE, SINGLE, NONE;

		static Dash parse(String input) {
			if (input.startsWith("--")) {
				return DOUBLE;
			} else if (input.startsWith("-")) {
				return SINGLE;
			} else {
				return NONE;
			}
		}

		private <T> T doubleSingleNone(T dbl, T sgl, T none) {
			// @formatter:off
			switch (this) {
			case DOUBLE: return dbl;
			case SINGLE: return sgl;
			case NONE: return none;
			default: throw Unhandled.enumException(this);
			}
			// @formatter:on
		}

		public int length() {
			return doubleSingleNone(2, 1, 0);
		}

		@Override
		public String toString() {
			return doubleSingleNone("--", "-", "");
		}
	}

	/** Models a single line within an eclipse.ini. */
	private static class Line {
		final Dash dash;
		final String content;

		private Line(Dash dash, String content) {
			this.dash = dash;
			this.content = content;
		}

		@Override
		public boolean equals(Object otherObj) {
			if (otherObj instanceof Line) {
				Line other = (Line) otherObj;
				return dash == other.dash && content.equals(other.content);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(dash, content);
		}

		@Override
		public String toString() {
			return dash.toString() + content;
		}

		static Line parse(String input) {
			Dash dash = Dash.parse(input);
			return new Line(dash, input.substring(dash.length()));
		}
	}

	List<Line> lines = new ArrayList<>();

	/** Parses an eclipse.ini from the given file. */
	public static EclipseIni parseFrom(File file) throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
			EclipseIni ini = new EclipseIni();
			String line;
			while ((line = reader.readLine()) != null) {
				ini.lines.add(Line.parse(line));
			}
			return ini;
		}
	}

	/** Writes this eclipse.ini out to a file. */
	public void writeTo(File file) throws FileNotFoundException {
		try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.ISO_8859_1))) {
			for (Line line : lines) {
				writer.println(line.toString());
			}
		}
	}

	/** Returns the line after the given line, and ensures that the returned value does not start with a dash. */
	public String getAfter(String input) {
		Line line = Line.parse(input);
		int i = lines.indexOf(line);
		Line result = lines.get(i + 1);
		Preconditions.checkState(result.dash == Dash.NONE, "Expected no dashes, was %s", result);
		return result.content;
	}

	/** Returns all lines as an array. */
	public String[] getLinesAsArray() {
		return lines.stream()
				.map(Line::toString)
				.collect(Collectors.toList())
				.toArray(new String[lines.size()]);
	}

	/** Sets a property, replacing its existing value or inserting just before vmargs. */
	public void set(String key, String value) {
		Line keyLine = Line.parse(key);
		int idx = lines.indexOf(keyLine);
		if (idx != -1) {
			// remove the old key
			lines.remove(idx);
			// and if there was a value, remove the value
			if (lines.size() > idx && lines.get(idx).dash == Dash.NONE) {
				lines.remove(idx);
			}
		} else {
			// make sure we do it before the vmargs
			idx = lines.indexOf(VM_ARGS);
			if (idx == -1) {
				idx = lines.size();
			}
		}
		lines.add(idx, keyLine);
		lines.add(idx + 1, Line.parse(value));
	}

	/** Sets the given property to a file. */
	public void set(String key, File file) {
		set(key, file.getAbsolutePath().replace('\\', '/'));
	}

	/** Sets the vmargs arguments, such as `-Xmx2g` to set the maximum heap size. */
	public void vmargs(String... vmargs) {
		int idx = lines.indexOf(VM_ARGS);
		if (idx != -1) {
			lines = lines.subList(0, idx);
		}
		lines.add(VM_ARGS);
		for (String vmarg : vmargs) {
			lines.add(Line.parse(vmarg));
		}
	}

	private static final Line VM_ARGS = new Line(Dash.SINGLE, "vmargs");
}
