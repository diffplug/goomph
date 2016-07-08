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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import groovy.util.Node;
import groovy.xml.XmlUtil;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.collect.ListMultimap;
import com.diffplug.common.collect.Multimaps;
import com.diffplug.common.io.Files;
import com.diffplug.gradle.FileMisc;

/**
 * Models an eclipse utility application and all of
 * its input state.
 *
 * Specifically targets utilities which are
 * only available as eclipse applications, such as
 * `org.eclipse.ant.core.antRunner` or
 * `org.eclipse.equinox.p2.director`.
 *
 * To run an `EclipseApp`, call {@link #runUsing(EclipseRunner)}
 * and pass an {@link EclipseRunner} instance.
 **
 * ```java
 * EclipseApp p2director = new EclipseApp("org.eclipse.equinox.p2.director");
 * p2director.addArg("repository", "http://somerepo");
 * p2director.addArg("destination", "file://somefile");
 * p2director.addArg("installIU", "org.eclipse.jdt");
 * p2director.addArg("installIU", "org.eclipse.text");
 * p2director.runUsing(new NativeRunner(eclipseLauncherExe));
 * ```
 *
 * will turn into
 *
 * ```
 * eclipsec.exe -application org.eclipse.equinox.p2.director
 *     -repository http://somerepo
 *     -destination file://somefile
 *     -installIU org.eclipse.jdt,org.eclipse.text
 * ```
 *
 * ## Staleness
 *
 * Many of these applications are deterministic - if you
 * pass them the same inputs, they generate the same outputs.
 *
 * To build fast staleness checking, use {@link #completeState()}
 * to get a String which contains the full state of the
 * application.
 *
 *
 * ## State besides arguments
 *
 * Sometimes, running an eclipse utility application includes state
 * besides its console arguments, such as the input `build.xml` for
 * `org.eclipse.ant.core.antRunner`.
 *
 * Ideally, this state should be included within the `EclipseApp` instance.
 * This can be accomplished by overriding {@link AntRunner#runUsing(EclipseRunner)},
 * setting up any state that the application requires, and cleaning up the state
 * after it has completed.
 *
 * See {@link AntRunner} for an example.
 */
public class EclipseApp {
	/**
	 * Creates an EclipseApp which will call the given application,
	 * such as `org.eclipse.ant.core.antRunner` or `org.eclipse.equinox.p2.director`
	 */
	public EclipseApp(String application) {
		addArg("application", application);
	}

	/** Runs this app using the given runner. */
	public void runUsing(EclipseRunner runner) throws Exception {
		runner.run(toArgList());
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

	//////////////////////////////
	// Basic arg infrastructure //
	//////////////////////////////
	protected final ListMultimap<String, String> args = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);

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
		args.put(key, FileMisc.quote(value));
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

	/**
	 * Models the `org.eclipse.ant.core.antRunner` application, including its `build.xml`.
	 *
	 * [Ant task documentation](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm)
	 */
	public static class AntRunner extends EclipseApp {
		public static final String ID = "org.eclipse.ant.core.antRunner";

		public AntRunner() {
			super(ID);
		}

		/**
		 * Saves the buildfile to a temporary file,
		 * runs the task, then deletes it.
		 */
		@Override
		public void runUsing(EclipseRunner runner) throws Exception {
			File tempFile = File.createTempFile("goomph-ant-build", ".xml");
			Files.write(buildXml().getBytes(StandardCharsets.UTF_8), tempFile);
			try {
				List<String> args = new ArrayList<>();
				args.addAll(toArgList());
				args.add("-buildfile");
				args.add(tempFile.getAbsolutePath());
				runner.run(args);
			} finally {
				FileMisc.forceDelete(tempFile);
			}
		}

		String buildXml = "NOBODY CALLED EclipseArgsBuilder.Ant.setTask";

		/** Sets the XML task node which will be called by this ant task. */
		public void setTask(Node taskNode) {
			Node project = new Node(null, "project");
			project.append(taskNode);
			buildXml = FileMisc.toUnixNewline(XmlUtil.serialize(project));
		}

		/** Defines a property for the ant task. */
		public void define(String key, String value) {
			addArg("D" + key + "=" + FileMisc.quote(value));
		}

		/** Defines a property to a file for the ant task. */
		public void defineToFile(String key, File value) {
			define(key, value.getAbsolutePath());
		}

		/** Returns the underlying buildXml. */
		protected String buildXml() {
			return Objects.requireNonNull(buildXml);
		}

		/** Includes the full state of both the args and the build.xml. */
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
