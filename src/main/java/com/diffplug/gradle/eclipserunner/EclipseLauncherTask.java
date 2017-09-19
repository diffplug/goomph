package com.diffplug.gradle.eclipserunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Files;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.p2.ParsedJar;

public class EclipseLauncherTask extends DefaultTask {
	@Input
	private List<String> mavenCoords = new ArrayList<>();

	@Input
	private List<String> projectPaths = new ArrayList<>();

	private List<Project> projects = new ArrayList<>();

	public List<String> getMavenCoords() {
		return mavenCoords;
	}

	public List<String> getProjectPaths() {
		return mavenCoords;
	}

	/**
	 * Adds a maven coordinate 'group:artifact:version'
	 */
	public void add(String coord) {
		mavenCoords.add(coord);
	}

	/**
	 * Adds a subproject to compile and run.
	 */
	public void add(Project depProject) {
		getDependsOn().add(depProject.getTasks().getByName(JavaPlugin.JAR_TASK_NAME));
		projectPaths.add(depProject.getPath());
		projects.add(depProject);
	}

	@Input
	private File workingDir;

	@Input
	private List<String> args;

	public File getWorkingDir() {
		return workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	@OutputDirectory
	public File output;

	public void setOutput(File output) {
		this.output = output;
	}

	public File getOutput() {
		return output;
	}

	@TaskAction
	public void run() throws Exception {
//		// resolve dependencies (
//		Dependency[] deps = mavenCoords.stream()
//				.map(getProject().getDependencies()::create)
//				.toArray(Dependency[]::new);
//		Configuration config = getProject().getRootProject().getConfigurations().detachedConfiguration(deps);
//		config.setDescription(mavenCoords.toString());
//		Set<File> plugins = config.resolve();

		Preconditions.checkArgument(mavenCoords.isEmpty());
		Preconditions.checkArgument(projects.size() == 1);
		Project project = projects.iterator().next();
		Configuration config = project.getConfigurations().getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME);
		Set<File> plugins = config.resolve();

		FileMisc.cleanDir(output);
		File pluginsDir = new File(output, "plugins");
		pluginsDir.mkdirs();

		for (File plugin : plugins) {
			ParsedJar parsed = new ParsedJar(plugin);
			String name = parsed.getSymbolicName() + "_" + parsed.getVersion() + ".jar";
			Files.copy(plugin, new File(pluginsDir, name));
		}

		JarFolderRunnerExternalJvm toRun = new JarFolderRunnerExternalJvm(output, getProject());
		toRun.run(Arrays.asList());
	}

	private static final Logger logger = Logger.getLogger(EclipseLauncherTask.class.getName());
}
