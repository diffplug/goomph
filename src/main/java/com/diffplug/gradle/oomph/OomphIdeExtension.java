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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.internal.Actions;
import org.gradle.plugins.ide.eclipse.GenerateEclipseProject;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.io.Files;
import com.diffplug.common.primitives.Booleans;
import com.diffplug.common.swt.os.OS;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.ConfigMisc;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.Lazyable;
import com.diffplug.gradle.StateBuilder;
import com.diffplug.gradle.eclipserunner.EclipseIni;
import com.diffplug.gradle.p2.P2Declarative;
import com.diffplug.gradle.p2.P2Model;
import com.diffplug.gradle.p2.P2Model.DirectorApp;
import com.diffplug.gradle.pde.EclipseRelease;
import com.diffplug.gradle.pde.PdeInstallation;

/** DSL for {@link OomphIdePlugin}. */
public class OomphIdeExtension implements P2Declarative {
	public static final String NAME = "oomphIde";

	final Project project;
	final WorkspaceRegistry workspaceRegistry;
	final SortedSet<File> projectFiles = new TreeSet<>();
	final Map<String, Object> workspaceFiles = new HashMap<>();
	final Map<String, Action<Map<String, String>>> workspaceProps = new HashMap<>();
	final P2Model p2 = new P2Model();
	final Lazyable<List<SetupAction>> setupActions = Lazyable.ofList();

	@Nonnull
	String name;
	@Nonnull
	String perspective;
	@Nonnull
	Object ideDir = "build/oomph-ide" + FileMisc.macApp();
	@Nonnull
	Action<DirectorApp> directorModifier = Actions.doNothing();
	@Nonnull
	Action<DirectorApp> runP2Using;

	Action<EclipseIni> eclipseIni;

	Object icon, splash;

	public OomphIdeExtension(Project project) throws IOException {
		this.project = Objects.requireNonNull(project);
		this.workspaceRegistry = WorkspaceRegistry.instance();
		this.name = project.getRootProject().getName();
		this.perspective = Perspectives.RESOURCES;
		this.runP2Using = app -> Errors.rethrow().run(() -> app.runUsingBootstrapper(project));
	}

	/** Returns the underlying project. */
	public Project getProject() {
		return project;
	}

	/** Returns the P2 model so that users can add the features they'd like. */
	@Override
	public P2Model getP2() {
		return p2;
	}

	/** Allows for fine-grained manipulation of the mirroring operation. */
	public void p2director(Action<DirectorApp> directorModifier) {
		this.directorModifier = Objects.requireNonNull(directorModifier);
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

	/** Sets the folder where the ide will be built. */
	public void ideDir(Object ideDir) {
		this.ideDir = Objects.requireNonNull(ideDir);
	}

	/** Adds all eclipse projects from all gradle projects. */
	public void addAllProjects() {
		addAllProjects(Specs.satisfyAll());
	}

	/** Adds all eclipse projects from all gradle projects whose paths meet the given spec. */
	public void addAllProjects(Spec<String> include) {
		project.getRootProject().getAllprojects().forEach(p -> {
			// this project is automatically included by logic
			// in OomphIdePlugin
			if (p == project) {
				return;
			}
			// this project depends on all the others
			if (include.isSatisfiedBy(p.getPath())) {
				addDependency(project.evaluationDependsOn(p.getPath()));
			}
		});
	}

	/** Adds the eclipse project from the given project path. */
	public void addProject(String projectPath) {
		addDependency(project.evaluationDependsOn(projectPath));
	}

	private static final String DOT_PROJECT = ".project";

	/** Adds the eclipse tasks from the given project as a dependency of our IDE setup task. */
	void addDependency(Project eclipseProject) {
		Task ideSetup = project.getTasks().getByName(OomphIdePlugin.IDE_SETUP_WORKSPACE);
		eclipseProject.getTasks().all(task -> {
			if ("eclipse".equals(task.getName())) {
				ideSetup.dependsOn(task);
			}
			if (task instanceof GenerateEclipseProject) {
				File projectFile = ((GenerateEclipseProject) task).getOutputFile();
				Preconditions.checkArgument(projectFile.getName().equals(DOT_PROJECT), "Project file must be '" + DOT_PROJECT + "', was %s", projectFile);
				projectFiles.add(projectFile);
			}
		});
	}

	/** Adds the given folder as an eclipse project. */
	public void addProjectFolder(Object folderObj) {
		File folder = project.file(folderObj);
		Preconditions.checkArgument(folder.isDirectory(), "Folder '%s' must be a directory containing a '" + DOT_PROJECT + "' file.");
		projectFiles.add(new File(folder, DOT_PROJECT));
	}

	private File getIdeDir() {
		return project.file(ideDir);
	}

	private File getWorkspaceDir() {
		return workspaceRegistry.workspaceDir(project, getIdeDir());
	}

	/** Sets the given path within the workspace directory to be a copy of the file located at fileSrc. */
	public void workspaceFile(String destination, Object fileSrc) {
		workspaceFiles.put(destination, fileSrc);
	}

	/** Sets the given path within the workspace directory to be a property file. */
	@SuppressWarnings("unchecked")
	public void workspaceProp(String destination, Action<Map<String, String>> configSupplier) {
		workspaceProps.merge(destination, configSupplier, (before, after) -> Actions.composite(before, after));
	}

	/** Adds an action which will be run inside our running application. */
	public void addSetupAction(SetupAction internalSetupAction) {
		setupActions.getRoot().add(internalSetupAction);
	}

	/** Eventually adds some actions which will be run inside our running application. */
	public void addSetupActionLazy(Action<List<SetupAction>> lazyInternalSetupAction) {
		setupActions.addLazyAction(lazyInternalSetupAction);
	}

	/** Links the given target into the workspace with the given name, see [eclipse manual](http://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.user%2Fconcepts%2Fconcepts-13.htm). */
	public void linkedResource(String linkName, Object linkTarget) {
		final String CORE_RES_PREFS_FILE = ".metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.core.resources.prefs";
		final String WS_PATHVAR_FMT = "pathvariable.%s";
		workspaceProp(CORE_RES_PREFS_FILE, props -> {
			//Eclipse cannot handle backslashes in this value.  It expects path separators to be '/'
			props.put(String.format(WS_PATHVAR_FMT, linkName), project.file(linkTarget).getAbsolutePath().replace("\\", "/"));
		});
	}

	////////////////
	// ideSetupP2 //
	////////////////
	static final String STALE_TOKEN = "token_stale";

	/** Returns the full state of the installation, but not the workspace. */
	String p2state() {
		StateBuilder state = new StateBuilder(project);
		state.add("ideDir", getIdeDir());
		state.add("p2", p2);
		state.addFile("icon", icon);
		state.addFile("splash", splash);
		state.add("name", name);
		state.add("perspective", perspective);
		return state.toString();
	}

	/** Returns true iff the installation is clean. */
	boolean p2isClean() throws IOException {
		return FileMisc.hasToken(getIdeDir(), STALE_TOKEN, p2state());
	}

	/** Creates or updates the installed plugins in this model. */
	void ideSetupP2() throws Exception {
		if (p2isClean()) {
			return;
		}
		File ideDir = getIdeDir();
		// clean the p2 folder, because p2director can't update anything
		FileMisc.cleanDir(ideDir);

		P2Model p2cached = new P2Model();
		p2cached.addArtifactRepoBundlePool();
		p2cached.copyFrom(p2);
		DirectorApp app = p2cached.directorApp(ideDir, "OomphIde");
		app.consolelog();
		// share the install for quickness
		app.bundlepool(GoomphCacheLocations.bundlePool());
		// create the native launcher
		app.platform(SwtPlatform.getRunning());
		// make any other modifications we'd like to make
		directorModifier.execute(app);

		// create it
		runP2Using.execute(app);
		// write out the branding product
		writeBrandingPlugin(ideDir);
		// setup the eclipse.ini file
		setupEclipseIni(ideDir);
		// write out a staleness token
		FileMisc.writeToken(ideDir, STALE_TOKEN, p2state());
	}

	/** Defaults to {@link DirectorApp#runUsingBootstrapper()} - this allows you to override that behavior. */
	public void runP2Using(Action<DirectorApp> runUsing) {
		this.runP2Using = runUsing;
	}

	/** Provisions using the given version of the full Eclipse PDE. */
	public void runP2UsingPDE(String version) {
		runP2Using(directorApp -> Errors.rethrow().run(() -> {
			directorApp.runUsing(PdeInstallation.from(EclipseRelease.official(version)));
		}));
	}

	/** Provisions using the latest available version of the full Eclipse PDE. */
	public void runP2UsingPDE() {
		runP2UsingPDE(EclipseRelease.LATEST);
	}

	private BufferedImage loadImg(Object obj) throws IOException {
		File file = project.file(obj);
		Preconditions.checkArgument(file.isFile(), "Image file %s does not exist!", file);
		return ImageIO.read(project.file(obj));
	}

	void writeBrandingPlugin(File ideDir) throws IOException {
		// load iconImg and splashImg
		BufferedImage iconImg, splashImg;
		int numSet = Booleans.countTrue(icon != null, splash != null);
		if (numSet == 0) {
			// nothing is set, use Goomph
			iconImg = BrandingProductPlugin.getGoomphIcon();
			splashImg = BrandingProductPlugin.getGoomphSplash();
		} else if (numSet == 1) {
			// anything is set, use it for everything 
			iconImg = loadImg(Optional.ofNullable(icon).orElse(splash));
			splashImg = iconImg;
		} else if (numSet == 2) {
			// both are set, use them each 
			iconImg = loadImg(icon);
			splashImg = loadImg(splash);
		} else {
			throw Unhandled.integerException(numSet);
		}

		File branding = new File(ideDir, FileMisc.macContentsEclipse() + "dropins/com.diffplug.goomph.branding");
		BrandingProductPlugin.create(branding, splashImg, iconImg, name, perspective);
		File bundlesInfo = new File(ideDir, FileMisc.macContentsEclipse() + "configuration/org.eclipse.equinox.simpleconfigurator/bundles.info");
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

	///////////////////////
	// ideSetupWorkspace //
	///////////////////////
	/** Returns true iff the workspace already exists. */
	boolean workspaceExists() {
		File workspaceDir = getWorkspaceDir();
		return workspaceDir.isDirectory() && !FileMisc.list(workspaceDir).isEmpty();
	}

	/** Sets up an IDE as described in this model from scratch. */
	void ideSetupWorkspace() throws Exception {
		if (workspaceExists()) {
			project.getLogger().lifecycle("Skipping " + OomphIdePlugin.IDE_SETUP_WORKSPACE + " because it already exists, run " + OomphIdePlugin.IDE_CLEAN + " to force a rebuild.");
		}
		File workspaceDir = getWorkspaceDir();
		// else we've gotta set it up
		FileMisc.cleanDir(workspaceDir);
		// write the workspace files
		workspaceFiles.forEach((path, src) -> {
			File target = new File(workspaceDir, path);
			File srcFile = project.file(src);
			try {
				FileUtils.copyFile(srcFile, target);
			} catch (IOException e) {
				throw new GradleException("error for workspaceFile('" + path + "', '" + srcFile + "'), maybe the source file does not exist?", e);
			}
		});
		// for each prop, load the existing map, if any, and pass it to the actions
		workspaceProps.forEach((path, content) -> {
			File target = new File(workspaceDir, path);
			Map<String, String> initial;
			if (target.exists()) {
				initial = ConfigMisc.loadPropertiesFile(target);
			} else {
				initial = new LinkedHashMap<>();
				FileMisc.mkdirs(target.getParentFile());
			}
			try {
				content.execute(initial);
				Files.write(ConfigMisc.props(initial), target);
			} catch (IOException e) {
				throw new GradleException("error when writing workspaceProp '" + path + "'", e);
			}
		});
		// perform internal setup
		internalSetup(getIdeDir());
	}

	/** Performs setup actions with a running OSGi container. */
	private void internalSetup(File ideDir) throws IOException {
		// get the user setup actions
		List<SetupAction> list = setupActions.getResult();
		// add the project importer
		list.add(new ProjectImporter(projectFiles));
		// order the actions
		List<SetupAction> ordered = SetupAction.order(list);
		// save the workspace as the last step
		ordered.add(new SaveWorkspace());

		SetupWithinEclipse internal = new SetupWithinEclipse(ideDir, ordered);
		Errors.constrainTo(IOException.class).run(() -> JavaExecable.exec(project, internal));
	}

	/////////
	// ide //
	/////////
	/** Runs the IDE which was setup by {@link #ideSetupWorkspace()}. */
	void ide() throws IOException {
		// clean any stale workspaces
		workspaceRegistry.clean();
		// then launch
		String launcher = OS.getNative().winMacLinux("eclipse.exe", "Contents/MacOS/eclipse", "eclipse");
		String[] args = new String[]{getIdeDir().getAbsolutePath() + "/" + launcher};
		Runtime.getRuntime().exec(args, null, getIdeDir());
	}

	/** Cleans everything from p2 and workspace. */
	void ideClean() {
		FileUtils.deleteQuietly(getIdeDir());
		FileUtils.deleteQuietly(getWorkspaceDir());
	}

	/////////////////
	// Conventions //
	/////////////////
	/** Convenience methods for setting the style. */
	public void style(Action<ConventionStyle> action) {
		try (ConventionStyle convention = new ConventionStyle(this)) {
			action.execute(convention);
		}
	}

	/** Adds the java development tools. */
	public void jdt(Action<ConventionJdt> action) {
		try (ConventionJdt convention = new ConventionJdt(this)) {
			action.execute(convention);
		}
	}

	/** Adds the plugin-development environment, @see ConventionPde. */
	public void pde(Action<ConventionPde> action) {
		try (ConventionPde convention = new ConventionPde(this)) {
			action.execute(convention);
		}
	}
}
