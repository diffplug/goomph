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
package com.diffplug.gradle.oomph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.plugins.ide.eclipse.GenerateEclipseProject;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.ConfigMisc;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.eclipserunner.EclipseIni;
import com.diffplug.gradle.p2.P2Model;

/** DSL for {@link OomphIdePlugin}. */
public class OomphIdeExtension {
	public static final String NAME = "oomphIde";

	private final Project project;

	public OomphIdeExtension(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	private final P2Model p2 = new P2Model();

	/** Returns the P2 model so that users can add the features they'd like. */
	public P2Model getP2() {
		return p2;
	}

	private Action<EclipseIni> eclipseIni;

	/** Sets properties in the `eclipse.ini`. */
	public void eclipseIni(Action<EclipseIni> eclipseIni) {
		Preconditions.checkArgument(this.eclipseIni == null, "Can only set eclipseIni once");
		this.eclipseIni = eclipseIni;
	}

	private Action<OomphTargetPlatform> targetPlatform = null;

	/** Sets the targetplatform configuration. */
	public void targetplatform(Action<OomphTargetPlatform> targetPlatform) {
		Preconditions.checkArgument(this.targetPlatform == null, "Can only set targetplatform once");
		this.targetPlatform = targetPlatform;
	}

	private Object ideDir = "build/oomph-ide";

	/** Sets the folder where the ide will be built. */
	public void ideDir(Object ideDir) {
		this.ideDir = ideDir;
	}

	private String perspective = "org.eclipse.jdt.ui.JavaPerspective";

	/** Sets the default perspective. */
	public void perspective(String perspective) {
		this.perspective = perspective;
	}

	/** Adds all eclipse projects from all the gradle projects. */
	public void addAllProjects() {
		Task setupIde = project.getTasks().getByName(OomphIdePlugin.SETUP);
		project.getRootProject().getAllprojects().forEach(p -> {
			if (p == project) {
				return;
			}
			// this project depends on all the others
			project.evaluationDependsOn(p.getPath());
			// and on all of their eclipse tasks
			project.getTasks().whenTaskAdded(task -> {
				if ("eclipse".equals(task.getName())) {
					setupIde.dependsOn(task);
				}
				if (task instanceof GenerateEclipseProject) {
					addProjectFile(((GenerateEclipseProject) task).getOutputFile());
				}
			});
		});
	}

	private Set<File> projectFiles = new HashSet<>();

	/** Adds the given project file. */
	void addProjectFile(File projectFile) {
		Preconditions.checkArgument(projectFile.getName().equals(".project"), "Project file must be '.project', was %s", projectFile);
		projectFiles.add(projectFile);
		System.out.println("Add file");
	}

	private File getIdeDir() {
		return project.file(ideDir);
	}

	private File getWorkspaceDir() {
		return new File(getIdeDir(), "workspace");
	}

	private String state() {
		return getIdeDir().toString() + Objects.toString(targetPlatform) + p2 + projectFiles.hashCode();
	}

	private Map<String, Supplier<byte[]>> pathToContent = new HashMap<>();

	/** Sets the given path within the ide directory to be a property file. */
	public void configProps(String file, Action<Map<String, String>> configSupplier) {
		pathToContent.put(file, ConfigMisc.props(configSupplier));
	}

	/** Sets the theme to be the classic eclipse look. */
	public void classicTheme() {
		configProps("workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs", props -> {
			props.put("eclipse.preferences.version", "1");
			props.put("themeid", "org.eclipse.e4.ui.css.theme.e4_classic");
		});
	}

	static final String STALE_TOKEN = "token_stale";

	/** Sets up an IDE as described in this model from scratch. */
	void setup() throws Exception {
		File dir = getIdeDir();
		// if we've got a clean token, we're all good
		if (FileMisc.hasToken(dir, STALE_TOKEN, state())) {
			return;
		}
		// else we've gotta set it up
		FileMisc.cleanDir(dir);
		// now we can install the IDE
		p2.addArtifactRepoBundlePool();
		P2Model.DirectorApp app = p2.directorApp(dir, "OomphIde");
		app.consolelog();
		// share the install for quickness
		app.bundlepool(GoomphCacheLocations.bundlePool());
		// create the native launcher
		app.platform(SwtPlatform.getRunning());
		// create it
		app.runUsingBootstrapper(project);
		// set the application to use "${ide}/workspace"
		setInitialWorkspace();
		// setup the eclipse.ini file
		setupEclipseIni(dir);
		// setup any config files
		pathToContent.forEach((path, content) -> {
			File target = new File(dir, path);
			FileMisc.mkdirs(target.getParentFile());
			Errors.rethrow().run(() -> Files.write(content.get(), target));
		});
		// perform internal setup
		internalSetup(dir);
		// write out a staleness token
		FileMisc.writeToken(dir, STALE_TOKEN, state());
	}

	/** Sets the workspace directory. */
	private void setInitialWorkspace() throws IOException {
		File workspace = getWorkspaceDir();
		FileMisc.cleanDir(workspace);
		configProps("configuration/.settings/org.eclipse.ui.ide.prefs", map -> {
			map.put("eclipse.preferences.version", "1");
			map.put("MAX_RECENT_WORKSPACES", "5");
			map.put("RECENT_WORKSPACES", workspace.getAbsolutePath());
			map.put("RECENT_WORKSPACES_PROTOCOL", "3");
			map.put("SHOW_RECENT_WORKSPACES", "false");
			map.put("SHOW_WORKSPACE_SELECTION_DIALOG", "false");
		});
		// turn off quickstarts and tipsAndTricks
		configProps("workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.ide.prefs", map -> {
			map.put("eclipse.preferences.version", "1");
			map.put("quickStart", "false");
			map.put("tipsAndTricks", "false");
		});
	}

	/** Sets the eclipse.ini file. */
	private void setupEclipseIni(File ideDir) throws FileNotFoundException, IOException {
		File iniFile = new File(ideDir, "eclipse.ini");
		EclipseIni ini = EclipseIni.parseFrom(iniFile);
		ini.set("-data", getWorkspaceDir());
		if (eclipseIni != null) {
			eclipseIni.execute(ini);
		}
		ini.writeTo(iniFile);
	}

	/** Performs setup actions with a running OSGi container. */
	private void internalSetup(File ideDir) throws IOException {
		project.getLogger().lifecycle("Internal setup");
		SetupWithinEclipse internal = new SetupWithinEclipse(ideDir);
		internal.add(new ProjectImporter(projectFiles));
		if (perspective != null) {
			internal.add(new PerspectiveSetter(perspective));
		}
		// setup the targetplatform
		if (targetPlatform != null) {
			OomphTargetPlatform targetPlatformInstance = new OomphTargetPlatform(project);
			targetPlatform.execute(targetPlatformInstance);
		}
		Errors.constrainTo(IOException.class).run(() -> JavaExecable.exec(project, internal));
	}

	/** Runs the IDE which was setup by {@link #setup()}. */
	void run() throws IOException {
		String cmd = OS.getNative().winMacLinux("eclipse.exe", "eclipse", "eclipse");
		Runtime.getRuntime().exec(cmd, new String[0], getIdeDir());
	}
}
