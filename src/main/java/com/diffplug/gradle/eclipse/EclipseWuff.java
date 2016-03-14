/*
 * Copyright 2015 DiffPlug
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
package com.diffplug.gradle.eclipse;

import java.io.File;

import org.gradle.api.Project;
import org.osgi.framework.Version;

import com.google.common.base.Preconditions;

import com.diffplug.common.base.Comparison;
import com.diffplug.common.swt.os.OS;

/** Represents an Eclipse which was downloaded by WUFF.  Assumes that the VER_ECLIPSE property is set. */
public class EclipseWuff {
	/** Returns the directory in which wuff caches its things. */
	private static File getWuffDir() {
		return new File(System.getProperty("user.home") + "/.wuff");
	}

	private final String version;
	private final Version versionOsgi;

	/** Wraps up the Wuff-downloaded eclipses. */
	public EclipseWuff(Project project) {
		version = (String) project.getProperties().get("VER_ECLIPSE");
		versionOsgi = Version.parseVersion(version);
		Preconditions.checkNotNull(version);
	}

	/** Returns the version for this Eclipse instance. */
	public String getVersion() {
		return version;
	}

	/** Returns the version for this Eclipse instance. */
	public Version getVersionOsgi() {
		return versionOsgi;
	}

	/** Returns the SDK directory for this version for this OS. */
	public File getSdkDir() {
		return getWuffDir().toPath().resolve("unpacked/eclipse-SDK-" + version + "-" + getSuffixForThisOS()).toFile();
	}

	/** Returns the given path within the SDK. */
	public File getSdkFile(String subPath) {
		return getSdkDir().toPath().resolve(subPath).toFile();
	}

	/** Returns the suffix used for the Eclipse directories for this OS. */
	private static String getSuffixForThisOS() {
		String root = OS.getNative().winMacLinux("win32", "macosx-cocoa", "linux-gtk");
		return OS.getNative().getArch().x86x64(root, root + "-x86_64");
	}

	/** Returns a directory containing a delta pack. */
	public File getDeltaPackDir() {
		return getWuffDir().toPath().resolve("unpacked/eclipse-" + version + "-delta-pack").toFile();
	}

	private static final Version MARS = Version.parseVersion("4.5.0");

	/** Returns the eclipse GUI executable. */
	public File getEclipseExecutable() {
		OS os = OS.getNative();
		if (os.isWindows()) {
			return getSdkFile("eclipse.exe");
		} else if (os.isMac()) {
			String pre45 = "eclipse.app";
			String after45 = "Contents/MacOS/eclipse";
			String path = Comparison.compare(versionOsgi, MARS).lesserEqualGreater(pre45, after45, after45);
			return getSdkFile(path);
		} else {
			return getSdkFile("eclipse");
		}
	}

	/** Returns the eclipse console executable. */
	public File getEclipseConsoleExecutable() {
		OS os = OS.getNative();
		if (os.isWindows()) {
			return getSdkFile("eclipsec.exe");
		} else if (os.isMac()) {
			String pre45 = "eclipse.app/Contents/MacOS/eclipse";
			String after45 = "Contents/MacOS/eclipse";
			String path = Comparison.compare(versionOsgi, MARS).lesserEqualGreater(pre45, after45, after45);
			return getSdkFile(path);
		} else {
			return getSdkFile("eclipse");
		}
	}
}
