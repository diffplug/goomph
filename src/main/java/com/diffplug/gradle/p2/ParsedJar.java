package com.diffplug.gradle.p2;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

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

	public ParsedJar(File osgiJar) throws IOException {
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
				System.err.println(osgiJar.getAbsolutePath() + " has no manifest.  Guessing name=" + symbolicName + " and version=" + version);
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
}
