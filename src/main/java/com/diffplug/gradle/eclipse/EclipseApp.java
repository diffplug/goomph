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
package com.diffplug.gradle.eclipse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.ListMultimap;
import com.diffplug.common.collect.Multimaps;
import com.diffplug.common.io.Files;

import groovy.util.Node;
import groovy.xml.XmlUtil;

/**
 * Models an eclipse utility application which can be run.
 *
 * Specifically targets utilities which are
 * only available as eclipse applications, such as
 * `org.eclipse.ant.core.antRunner` or
 * `org.eclipse.equinox.p2.director`.
 *
 * To run an `EclipseApp`, call {@link #runUsing(Runner)}
 * and pass a {@link Runner} instance.
 *
 * Generally, these applications are deterministic - if you
 * pass them the same inputs, they generate the same outputs.
 *
 * To enable fast staleness checking, use {@link #completeState()}
 * to get a String which contains the full state of the
 * application.  The output of an `EclipseApp` is be a
 * function of its `completeState()`.
 *
 * ```java
 * EclipseApp p2director = new EclipseApp("org.eclipse.equinox.p2.director");
 * p2director.addArg("repository", "http://somerepo");
 * p2director.addArg("destination", "file://somefile");
 * p2director.addArg("installIU", "org.eclipse.jdt");
 * p2director.addArg("installIU", "org.eclipse.text");
 * p2director.runUsing(new EclipsecRunner(folderWhereEclipseLives));
 * ```
 *
 * will turn into
 *
 * ```
 * eclipsec -application org.eclipse.equinox.p2.director
 *     -repository http://somerepo
 *     -destination file://somefile
 *     -installIU org.eclipse.jdt,org.eclipse.text
 * ```
 */
public class EclipseApp {
	/**
	 * Runs the given args using a headless eclipse instance.
	 *
	 * The major implementations are TODO: list.
	 */
	public static interface Runner {
		void run(List<String> args) throws Exception;
	}

	/** Runs these args using the given runner. */
	public void runUsing(Runner runner) throws Exception {
		runner.run(toArgList());
	}

	public EclipseApp(String application) {
		addArg("application", application);
	}

	/**
	 * Writes out the entire state of this argsbuilder
	 * to a string.  This can be used to determine if
	 * the arguments have changed at all, to aid in staleness checking.
	 *
	 * If you extend EclipseArgsBuilder and add any kinds of
	 * new state (e.g. EclipseAntArgsBuilder), then you
	 * *must* override this method and embed all
	 * internal state within it.
	 */
	public String completeState() {
		StringBuilder builder = new StringBuilder();
		args.asMap().forEach((key, values) -> {
			builder.append("-" + key);
			boolean valuesIsEmptyString = values.size() == 1 && Iterables.getOnlyElement(values).isEmpty();
			if (!valuesIsEmptyString) {
				builder.append(' ');
				builder.append(Joiner.on(",").join(values));
			}
			builder.append('\n');
		});
		return builder.toString();
	}

	@Override
	public String toString() {
		return completeState();
	}

	////////////
	// Basic arg infrastructure //
	private final ListMultimap<String, String> args = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);

	/**
	 * `addArg("flag", "value")` will add `-flag value` to command line.
	 * 
	 * ```java
	 * addArg("flag", "A")
	 * addArg("flag", "B")
	 * ```
	 * 
	 * will add `-flag A,B` to the command line.
	 */
	public void addArg(String key, String value) {
		if (value.contains(" ")) {
			value = "\"" + value + "\"";
		}
		args.put(key, value);
	}

	/** `addArg("flag")` will add "-flag" to command line. */
	public void addArg(String key) {
		// no reason to set a flag twice
		if (!args.get(key).equals(Collections.singletonList(""))) {
			args.put(key, "");
		}
	}

	/** Returns the args. */
	protected List<String> toArgList() {
		List<String> argList = new ArrayList<>(args.size() * 2);
		args.asMap().forEach((key, values) -> {
			argList.add("-" + key);
			boolean valuesIsEmptyString = values.size() == 1 && Iterables.getOnlyElement(values).isEmpty();
			if (!valuesIsEmptyString) {
				argList.add(Joiner.on(",").join(values));
			}
		});
		return argList;
	}

	///////////////////////////////////////
	// Aliases for common addArg() calls //
	///////////////////////////////////////
	/**
	 * Any cached data used by the OSGi framework and eclipse runtime will be wiped clean. This will clean the caches used to store bundle dependency resolution and eclipse extension registry data. Using this option will force eclipse to reinitialize these caches.
	 */
	public void clean() {
		addArg("clean");
	}

	/**
	 * Any log output is also sent to Java's System.out (typically back to the command shell if any).
	 */
	public void consolelog() {
		addArg("consolelog");
	}

	/** */
	public static class Ant extends EclipseApp {
		public Ant() {
			super("org.eclipse.ant.core.antRunner");
		}

		/**
		 * Saves the buildfile to a temporary file,
		 * runs the task, then deletes it.
		 */
		@Override
		public void runUsing(Runner runner) throws Exception {
			File tempFile = File.createTempFile("goomph-ant-build", ".xml");
			Files.write(buildXml().getBytes(StandardCharsets.UTF_8), tempFile);
			try {
				List<String> args = new ArrayList<>();
				args.addAll(toArgList());
				args.add("-buildfile");
				args.add(tempFile.getAbsolutePath());
				runner.run(args);
			} finally {
				tempFile.delete();
			}
		}

		String buildXml = "NOBODY CALLED EclipseArgsBuilder.Ant.setTask";

		/** Sets the XML node which will be called by this ant task. */
		public void setTask(Node taskNode) {
			Node project = new Node(null, "project");
			project.append(taskNode);
			buildXml = XmlUtil.serialize(project).replace("\r", "");
		}

		/** Defines a value for the ant task. */
		public void define(String key, String value) {
			if (value.contains(" ")) {
				value = "\"" + value + "\"";
			}
			addArg("D" + key + "=" + value);
		}

		/** Defines a value for the ant task. */
		public void defineToFile(String key, File value) {
			define(key, value.getAbsolutePath());
		}

		protected String buildXml() {
			return Objects.requireNonNull(buildXml);
		}

		/**
		 * Writes out the entire state of this argsbuilder
		 * to a string.  This can be used to determine if
		 * the arguments have changed at all, to aid in staleness checking.
		 *
		 * If you extend EclipseArgsBuilder and add any kinds of
		 * new state (e.g. EclipseAntArgsBuilder), then you
		 * *must* override this method and embed all
		 * internal state within it.
		 */
		@Override
		public String completeState() {
			StringBuilder builder = new StringBuilder();
			builder.append("### ARGS ###\n");
			builder.append(super.completeState());
			builder.append("\n### BUILD.XML ###\n");
			builder.append(buildXml());
			return builder.toString();
		}
	}
}
