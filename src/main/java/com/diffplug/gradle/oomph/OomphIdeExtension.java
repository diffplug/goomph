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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.gradle.api.Action;
import org.gradle.api.Project;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.ConfigMisc;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.oomph.internal.WorkspaceModel;
import com.diffplug.gradle.p2.P2Model;

/** DSL for {@link OomphIdePlugin}. */
public class OomphIdeExtension {
	public static final String NAME = "oomphIde";

	private final Project project;

	public OomphIdeExtension(Project project) {
		this.project = Objects.requireNonNull(project);
		classicTheme();
	}

	private final P2Model p2 = new P2Model();

	/** Returns the P2 model so that users can add the features they'd like. */
	public P2Model getP2() {
		return p2;
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

	private File getIdeDir() {
		return project.file(ideDir);
	}

	private File getWorkspaceDir() {
		return new File(getIdeDir(), "workspace");
	}

	private String state() {
		return getIdeDir().toString() + Objects.toString(targetPlatform) + p2;
	}

	private Map<String, Supplier<byte[]>> pathToContent = new HashMap<>();

	public void configProps(String file, Action<Map<String, String>> configSupplier) {
		pathToContent.put(file, ConfigMisc.props(configSupplier));
	}

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
		// point to the dependent projects
		createProjects();
		// TODO: update eclipse.ini

		if (targetPlatform != null) {
			OomphTargetPlatform targetPlatformInstance = new OomphTargetPlatform(project);
			targetPlatform.execute(targetPlatformInstance);
			// TODO: setup targetplatform in workspace
		}
		pathToContent.forEach((path, content) -> {
			File target = new File(dir, path);
			FileMisc.mkdirs(target.getParentFile());
			Errors.rethrow().run(() -> Files.write(content.get(), target));
		});
	}

	/** Sets the workspace directory. */
	private void setInitialWorkspace() throws IOException {
		File workspace = getWorkspaceDir();
		FileMisc.cleanDir(workspace);
		configProps("configuration/.settings/org.eclipse.ui.ide.prefs", map -> {
			map.put("MAX_RECENT_WORKSPACES", "5");
			map.put("RECENT_WORKSPACES", workspace.getAbsolutePath());
			map.put("RECENT_WORKSPACES_PROTOCOL", "3");
			map.put("SHOW_RECENT_WORKSPACES", "false");
			map.put("SHOW_WORKSPACE_SELECTION_DIALOG", "false");
			map.put("eclipse.preferences.version", "1");
		});
	}

	private void createProjects() throws IOException {
		File projectsDir = new File(getWorkspaceDir(), ".metadata/.plugins/org.eclipse.core.resources/.projects");

		String root = "C:\\Users\\ntwigg\\Documents\\DiffPlugDev\\talk-gradle_and_eclipse_rcp\\com.diffplug.";
		List<String> projs = Arrays.asList("needs17", "needs18", "needsBoth", "rcpdemo", "talks.rxjava_and_swt");
		for (String proj : projs) {
			File projDir = new File(root + proj);
			String name = projDir.getName();
			WorkspaceModel.writeProjectLocation(new File(projectsDir, name), projDir);
		}
	}

	/** Runs the IDE which was setup by {@link #setup()}. */
	void run() {

	}
}
