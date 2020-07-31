/*
 * Copyright (C) 2020 DiffPlug
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
package com.diffplug.gradle.p2;


import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.eclipserunner.EclipseApp;
import com.diffplug.gradle.pde.EclipseRelease;
import com.diffplug.gradle.pde.PdeInstallation;
import java.io.File;

/**
 * Models the CategoryPublisher application ([eclipse docs](https://wiki.eclipse.org/Equinox/p2/Publisher#Category_Publisher).
 */
public class CategoryPublisher extends EclipseApp {

	private final EclipseRelease eclipseRelease;

	/**
	 * Creates a CategoryPublisher
	 *
	 * @param eclipseRelease The eclipse release to be used to run the public application
	 * */
	public CategoryPublisher(EclipseRelease eclipseRelease) {
		super("org.eclipse.equinox.p2.publisher.CategoryPublisher");
		consolelog();
		this.eclipseRelease = eclipseRelease;
	}

	/** Compress the output index */
	public void compress() {
		addArg("compress");
	}

	/** Sets the given location to be the target for metadata. */
	public void metadataRepository(File file) {
		addArg("metadataRepository", FileMisc.asUrl(file));
	}

	/** Sets the given location of context metadata. */
	public void contextMetadata(File file) {
		addArg("contextMetadata", FileMisc.asUrl(file));
	}

	/** Sets the given location of the category definition. */
	public void categoryDefinition(File file) {
		addArg("categoryDefinition", FileMisc.asUrl(file));
	}

	/** Sets the given category qualifier */
	public void categoryQualifier(String categoryQualifier) {
		addArg("categoryQualifier", categoryQualifier);
	}

	public void runUsingPdeInstallation() throws Exception {
		runUsing(PdeInstallation.from(eclipseRelease));
	}
}
