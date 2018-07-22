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
package com.diffplug.gradle.osgi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.java.archives.ManifestException;
import org.gradle.api.java.archives.internal.ManifestInternal;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import groovy.lang.Closure;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.io.Files;

class LimitedManifest implements Manifest, ManifestInternal {
	final Jar jarTask;
	final Manifest old;
	final BndManifestExtension extension;

	LimitedManifest(Jar jarTask, BndManifestExtension extension) {
		this.jarTask = jarTask;
		this.old = jarTask.getManifest();
		this.extension = extension;
	}

	String getContent() throws Throwable {
		// find the location of the manifest in the output resources directory
		JavaPluginConvention javaConvention = jarTask.getProject().getConvention().getPlugin(JavaPluginConvention.class);
		SourceSet main = javaConvention.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		Path outputManifest = main.getOutput().getResourcesDir().toPath().resolve("META-INF/MANIFEST.MF");
		// if we don't want to merge, then delete the existing manifest so that bnd doesn't merge with it
		if (!extension.mergeWithExisting) {
			java.nio.file.Files.deleteIfExists(outputManifest);
		}
		// take the bnd action 
		String content = BndManifestPlugin.takeBndAction(jarTask.getProject(), jarTask, jar -> {
			Preconditions.checkState(jarTask.getManifest() == this, "Never call jar.setManifest() with osgiBndManifest");
			return StringPrinter.buildString(printer -> {
				try (OutputStream output = printer.toOutputStream(StandardCharsets.UTF_8)) {
					aQute.bnd.osgi.Jar.writeManifest(jar.getManifest(), printer.toOutputStream(StandardCharsets.UTF_8));
				} catch (Exception e) {
					throw Errors.asRuntime(e);
				}
			});
		});
		System.out.println("------------------");
		System.out.println(content);
		return content;
	}

	@Override
	public Manifest writeTo(Writer writer) {
		try {
			writer.write(getContent());
		} catch (Throwable e) {
			throw Errors.asRuntime(e);
		}
		return this;
	}

	@Override
	public Manifest writeTo(Object path) {
		File file = jarTask.getProject().file(path);
		try (Writer writer = Files.asByteSink(file).asCharSink(Charset.forName(charset)).openBufferedStream()) {
			writeTo(writer);
		} catch (IOException e) {
			throw Errors.asRuntime(e);
		}
		return this;
	}

	private String charset = StandardCharsets.UTF_8.name();

	////////////////////////////////////////////////////
	// ManifestInternal to avoid getting wrapped:
	// https://github.com/gradle/gradle/blob/a11a6e420c9712647fc1855df6d21329bf92ecdd/subprojects/platform-jvm/src/main/java/org/gradle/jvm/tasks/Jar.java#L69-L74 
	///////////////////////////////////////////////////
	@Override
	public String getContentCharset() {
		return charset;
	}

	@Override
	public void setContentCharset(String charset) {
		this.charset = charset;
	}

	@Override
	public Manifest writeTo(OutputStream out) {
		Errors.rethrow().run(() -> {
			out.write(getContent().getBytes(charset));
		});
		return this;
	}

	//////////////////////////////////////
	// delegates everything else to old //
	//////////////////////////////////////
	@Override
	public Manifest attributes(Map<String, ?> attributes) throws ManifestException {
		old.attributes(attributes);
		return this;
	}

	@Override
	public Attributes getAttributes() {
		return old.getAttributes();
	}

	@Override
	public Manifest attributes(Map<String, ?> arg0, String arg1) throws ManifestException {
		old.attributes(arg0, arg1);
		return this;
	}

	@Override
	public Map<String, Attributes> getSections() {
		return old.getSections();
	}

	// because we hijack attributes and don't support merging, this is always the "effective" manifest
	@Override
	public Manifest getEffectiveManifest() {
		return this;
	}

	@Override
	public Manifest from(Object... arg0) {
		old.from(arg0);
		return this;
	}

	@Override
	public Manifest from(Object arg0, Closure<?> arg1) {
		old.from(arg0, arg1);
		return this;
	}
}
