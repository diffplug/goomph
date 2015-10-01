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
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;

import com.diffplug.common.base.MoreCollectors;

/** Generates a command line which will execute PDE build. */
public class PdeBuild {
	private EclipseWuff eclipse;

	public PdeBuild(Project project) {
		this.eclipse = new EclipseWuff(project);
	}

	/** {eclipse}/plugins/org.eclipse.equinox.launcher_{somever}.jar */
	private File launcherJar() {
		List<File> files = Arrays.asList(eclipse.getSdkFile("plugins").listFiles());
		return files.stream()
				.filter(file -> file.getName().startsWith("org.eclipse.equinox.launcher_"))
				.filter(file -> file.getName().endsWith(".jar"))
				.filter(File::isFile)
				.collect(MoreCollectors.singleOrEmpty())
				.get();
	}

	/** {eclipse}/plugins/org.eclipse.pde.build_{somever}/ */
	private File pdeBuildFolder() {
		List<File> files = Arrays.asList(eclipse.getSdkFile("plugins").listFiles());
		return files.stream()
				.filter(file -> file.getName().startsWith("org.eclipse.pde.build_"))
				.filter(File::isDirectory)
				.collect(MoreCollectors.singleOrEmpty())
				.get();
	}

	/** {eclipse}/plugins/org.eclipse.pde.build_{somever}/scripts/productBuild/productBuild.xml */
	private File productBuildXml() {
		return pdeBuildFolder().toPath()
				.resolve("scripts/productBuild/productBuild.xml")
				.toFile();
	}

	/** Returns a command which will execute the PDE builder for a product. */
	public String productBuildCmd(File buildDir) {
		return antBuildCmd(productBuildXml()) + " -Dbuilder=" + quote(buildDir);
	}

	/** Returns a command which will execute the PDE builder for a generic ant build file. */
	public String antBuildCmd(File buildfile) {
		StringBuilder builder = new StringBuilder();
		builder.append("java -jar " + quote(launcherJar()));
		builder.append(" -application org.eclipse.ant.core.antRunner");
		builder.append(" -buildfile " + quote(buildfile));
		return builder.toString();
	}

	private static String quote(File toQuote) {
		return "\"" + toQuote.getAbsolutePath() + "\"";
	}
}
