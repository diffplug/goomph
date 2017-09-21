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
package com.diffplug.gradle.p2;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a jar file's name and version by first looking at
 * its manifest, then its filename.
 */
public class ParsedJar {
	private String symbolicName;
	private String version;
	private boolean isSource;

	public String getSymbolicName() {
		return symbolicName;
	}

	public String getVersion() {
		return version;
	}

	public boolean isSource() {
		return isSource;
	}

	public static ParsedJar parse(File file) {
		try {
			return new ParsedJar(file);
		} catch (Exception e) {
			logger.info("Unabled to parse jar " + file, e);
			return new ParsedJar(file.getName());
		}
	}

	private ParsedJar(String name) {
		this.symbolicName = name;
		this.version = "0.0.0";
		this.isSource = false;
	}

	private ParsedJar(File osgiJar) throws IOException {
		try (JarFile jarFile = new JarFile(osgiJar)) {
			if (jarFile.getManifest() != null) {
				Attributes attr = jarFile.getManifest().getMainAttributes();
				symbolicName = beforeSemicolon(attr.getValue("Bundle-SymbolicName"));
				version = attr.getValue("Bundle-Version");
				String source = attr.getValue("Eclipse-SourceBundle");
				if (source != null) {
					isSource = true;
					symbolicName = beforeSemicolon(source);
				} else {
					isSource = false;
				}
			} else {
				String name = osgiJar.getName();
				int lastUnderscore = name.lastIndexOf("_");
				symbolicName = name.substring(0, lastUnderscore);
				version = name.substring(lastUnderscore + 1);
				isSource = false;
				logger.warn(osgiJar.getAbsolutePath() + " has no manifest.  Guessing name=" + symbolicName + " and version=" + version);
			}
		}
	}

	/** Parses out a name from an OSGi manifest header. */
	private static String beforeSemicolon(String input) {
		int firstSemiColon = input.indexOf(';');
		if (firstSemiColon == -1) {
			return input.trim();
		} else {
			return input.substring(0, firstSemiColon).trim();
		}
	}

	@Override
	public String toString() {
		return "name=" + symbolicName + " version=" + version + " isSource=" + isSource;
	}

	private static Logger logger = LoggerFactory.getLogger(ParsedJar.class);
}
