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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.plugins.ide.eclipse.GenerateEclipseProject;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.collect.ImmutableMap;
import com.diffplug.common.io.Files;
import com.diffplug.common.primitives.Booleans;
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
	private final WorkspaceRegistry workspaceRegistry;
	private final SortedSet<File> projectFiles = new TreeSet<>();
	private final Map<String, Supplier<byte[]>> workspaceToContent = new HashMap<>();
	private final P2Model p2 = new P2Model();

	@Nonnull
	private String name;
	@Nonnull
	private String perspective;
	@Nonnull
	private Object ideDir = "build/oomph-ide" + FileMisc.macApp();

	private Action<EclipseIni> eclipseIni;

	private Object icon, splash;

	private String targetPlatformName;
	private Action<OomphTargetPlatform> targetPlatform;

	public OomphIdeExtension(Project project) throws IOException {
		this.project = Objects.requireNonNull(project);
		this.workspaceRegistry = WorkspaceRegistry.instance();
		this.name = project.getRootProject().getName();
		this.perspective = Perspectives.RESOURCES;
	}

	/** Returns the P2 model so that users can add the features they'd like. */
	public P2Model getP2() {
		return p2;
	}

	/** Sets the icon image - any size and format is okay, but something square is recommended. */
	public void icon(Object icon) {
		this.icon = Objects.requireNonNull(icon);
	}

	/** Sets the splash screen image - any size and format is okay. */
	public void splash(Object splash) {
		this.splash = Objects.requireNonNull(splash);
	}

	/** Sets the name of the generated IDE.  Defaults to the name of the root project. */
	public void name(String name) {
		this.name = Objects.requireNonNull(name);
	}

	/** Sets the starting perspective (window layout), see {@link Perspectives} for common perspectives. */
	public void perspective(String perspective) {
		this.perspective = Objects.requireNonNull(perspective);
	}

	/** Sets properties in the `eclipse.ini`. */
	public void eclipseIni(Action<EclipseIni> eclipseIni) {
		Preconditions.checkArgument(this.eclipseIni == null, "Can only set eclipseIni once");
		this.eclipseIni = eclipseIni;
	}

	/** Sets the targetplatform configuration. */
	public void targetplatform(String name, Action<OomphTargetPlatform> targetPlatform) {
		Preconditions.checkArgument(this.targetPlatform == null, "Can only set targetplatform once");
		this.targetPlatformName = Objects.requireNonNull(name);
		this.targetPlatform = Objects.requireNonNull(targetPlatform);
	}

	/** Sets the folder where the ide will be built. */
	public void ideDir(Object ideDir) {
		this.ideDir = Objects.requireNonNull(ideDir);
	}

	/** Adds all eclipse projects from all gradle projects. */
	public void addAllProjects() {
		Task setupIde = project.getTasks().getByName(OomphIdePlugin.IDE_SETUP);
		project.getRootProject().getAllprojects().forEach(p -> {
			if (p == project) {
				return;
			}
			// this project depends on all the others
			project.evaluationDependsOn(p.getPath());
			// and on all of their eclipse tasks
			p.getTasks().all(task -> {
				if ("eclipse".equals(task.getName())) {
					setupIde.dependsOn(task);
				}
				if (task instanceof GenerateEclipseProject) {
					addProjectFile(((GenerateEclipseProject) task).getOutputFile());
				}
			});
		});
	}

	/** Adds the given project file. */
	void addProjectFile(File projectFile) {
		Preconditions.checkArgument(projectFile.getName().equals(".project"), "Project file must be '.project', was %s", projectFile);
		projectFiles.add(projectFile);
	}

	private File getIdeDir() {
		return project.file(ideDir);
	}

	private File getWorkspaceDir() {
		return workspaceRegistry.workspaceDir(project, getIdeDir());
	}

	String state() {
		OomphTargetPlatform platformInstance = new OomphTargetPlatform(project);
		if (targetPlatform != null) {
			targetPlatform.execute(platformInstance);
		}
		return getIdeDir() + "\n" + platformInstance + "\n" + p2 + "\n" + projectFiles;
	}

	/** Sets the given path within the ide directory to be a property file. */
	public void workspaceProp(String file, Action<Map<String, String>> configSupplier) {
		workspaceToContent.put(file, ConfigMisc.props(configSupplier));
	}

	/** Sets the theme to be the classic eclipse look. */
	public void classicTheme() {
		workspaceProp(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs", props -> {
			props.put("eclipse.preferences.version", "1");
			props.put("themeid", "org.eclipse.e4.ui.css.theme.e4_classic");
		});
	}

	/** Sets a nice font and whitespace settings. */
	public void niceText() {
		niceText(OS.getNative().winMacLinux("9.0", "11.0", "10.0"));
	}

	/** Sets a nice font and whitespace settings. */
	public void niceText(String fontSize) {
		// visible whitespace
		workspaceProp(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.editors.prefs", props -> {
			props.put("eclipse.preferences.version", "1");
			props.put("showCarriageReturn", "false");
			props.put("showLineFeed", "false");
			props.put("showWhitespaceCharacters", "true");
		});
		// improved fonts
		workspaceProp(".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.ui.workbench.prefs", props -> {
			props.put("eclipse.preferences.version", "1");
			String font = OS.getNative().winMacLinux("Consolas", "Monaco", "Monospace");
			props.put("org.eclipse.jface.textfont", "1|" + font + "|" + fontSize + "|0|WINDOWS|1|-12|0|0|0|400|0|0|0|0|3|2|1|49|" + font);
		});
	}

	static final String STALE_TOKEN = "token_stale";

	/** Returns true iff the installation is clean. */
	boolean isClean() {
		return Errors.rethrow().get(() -> FileMisc.hasToken(getIdeDir(), STALE_TOKEN, state()));
	}

	/** Sets up an IDE as described in this model from scratch. */
	void ideSetup() throws Exception {
		if (isClean()) {
			return;
		}
		File ideDir = getIdeDir();
		File workspaceDir = getWorkspaceDir();
		// else we've gotta set it up
		FileMisc.cleanDir(ideDir);
		FileMisc.cleanDir(workspaceDir);
		// now we can install the IDE
		P2Model p2cached = p2.copy();
		p2cached.addArtifactRepoBundlePool();
		P2Model.DirectorApp app = p2cached.directorApp(ideDir, "OomphIde");
		app.consolelog();
		// share the install for quickness
		app.bundlepool(GoomphCacheLocations.bundlePool());
		// create the native launcher
		app.platform(SwtPlatform.getRunning());
		// create it
		app.runUsingBootstrapper(project);
		// set the application to use "${ide}/workspace"
		//setInitialWorkspace();
		// write out the branding product
		writeBrandingPlugin(ideDir);
		// setup the eclipse.ini file
		setupEclipseIni(ideDir);
		// setup any config files
		workspaceToContent.forEach((path, content) -> {
			File target = new File(workspaceDir, path);
			FileMisc.mkdirs(target.getParentFile());
			Errors.rethrow().run(() -> Files.write(content.get(), target));
		});
		// perform internal setup
		internalSetup(ideDir);
		// write out a staleness token
		FileMisc.writeToken(ideDir, STALE_TOKEN, state());
	}

	private BufferedImage loadImg(Object obj) throws IOException {
		return ImageIO.read(project.file(obj));
	}

	void writeBrandingPlugin(File ideDir) throws IOException {
		// load iconImg and splashImg
		BufferedImage iconImg, splashImg;
		int numSet = Booleans.countTrue(icon != null, splash != null);
		if (numSet == 0) {
			iconImg = BrandingProductPlugin.getGoomphIcon();
			splashImg = BrandingProductPlugin.getGoomphSplash();
		} else if (numSet == 1) {
			iconImg = loadImg(Optional.ofNullable(icon).orElse(splash));
			splashImg = iconImg;
		} else if (numSet == 2) {
			iconImg = loadImg(icon);
			splashImg = loadImg(splash);
		} else {
			throw Unhandled.integerException(numSet);
		}

		File branding = new File(ideDir, FileMisc.macContentsEclipse() + "dropins/com.diffplug.goomph.branding");
		BrandingProductPlugin.create(branding, splashImg, iconImg, ImmutableMap.of(
				"plugin.xml", str -> str.replace("%name%", name),
				"plugin_customization.ini", str -> str.replace("org.eclipse.jdt.ui.JavaPerspective", perspective)));
		File bundlesInfo = new File(ideDir, "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
		FileMisc.modifyFile(bundlesInfo, content -> {
			return content + "com.diffplug.goomph.branding,1.0.0,dropins/com.diffplug.goomph.branding/,4,true" + System.lineSeparator();
		});
	}

	/** Sets the eclipse.ini file. */
	private void setupEclipseIni(File ideDir) throws FileNotFoundException, IOException {
		File iniFile = new File(ideDir, FileMisc.macContentsEclipse() + "eclipse.ini");
		EclipseIni ini = EclipseIni.parseFrom(iniFile);
		ini.set("-data", getWorkspaceDir());
		ini.set("-product", "com.diffplug.goomph.branding.product");
		ini.set("-showsplash", "dropins/com.diffplug.goomph.branding/splash.bmp");
		// p2 director makes an invalid mac install out of the box.  Blech.
		if (OS.getNative().isMac()) {
			ini.set("-install", new File(ideDir, "Contents/MacOS"));
			ini.set("-configuration", new File(ideDir, "Contents/Eclipse/configuration"));
		}
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
		// setup the targetplatform
		if (targetPlatform != null) {
			OomphTargetPlatform targetPlatformInstance = new OomphTargetPlatform(project);
			targetPlatform.execute(targetPlatformInstance);
			internal.add(new TargetPlatformSetter(targetPlatformName, targetPlatformInstance.getInstallations()));
		}
		Errors.constrainTo(IOException.class).run(() -> JavaExecable.exec(project, internal));
	}

	/** Runs the IDE which was setup by {@link #ideSetup()}. */
	void ide() throws IOException {
		// clean any stale workspaces
		workspaceRegistry.clean();
		// then launch
		String launcher = OS.getNative().winMacLinux("eclipse.exe", "Contents/MacOS/eclipse", "eclipse");
		String[] args = new String[]{getIdeDir().getAbsolutePath() + "/" + launcher};
		Runtime.getRuntime().exec(args, null, getIdeDir());
	}
}
