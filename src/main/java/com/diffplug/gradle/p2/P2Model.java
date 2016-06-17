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
package com.diffplug.gradle.p2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.gradle.api.Action;
import org.gradle.api.XmlProvider;
import org.gradle.internal.Actions;
import org.gradle.internal.xml.XmlTransformer;
import org.gradle.listener.ActionBroadcast;

import groovy.util.Node;
import groovy.xml.XmlUtil;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.base.Errors;
import com.diffplug.common.collect.Sets;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.JavaExecable;
import com.diffplug.gradle.eclipse.EclipseArgsBuilder;
import com.diffplug.gradle.eclipse.EquinoxLauncher;

/**
 * Runs P2 director to install artifacts from P2 repositories.
 * 
 * WARNING: This part of Goomph currently has the following precondition:
 * your project must have the property VER_ECLIPSE=4.5.2 (or some other version),
 * and you must have installed that Eclipse using Wuff. We will remove this
 * restriction in the future.
 * 
 * ```groovy
 * import com.diffplug.gradle.*
 * import com.diffplug.gradle.pde.*
 * import com.diffplug.common.swt.os.*
 *
 * // list of OS values for which we want to create an installer
 * def INSTALLERS = OS.values()
 * def VER_JRE = '1.8.0.40'
 *
 * // add each of the core IUs
 * def coreModel = new P2DirectorModel()
 * coreModel.addIU(APP_ID)
 * coreModel.addIU('com.diffplug.jre.feature.group')
 * coreModel.addIU('com.diffplug.native.feature.group')
 *
 * // add each of the local repositories
 * def repoRoot = 'file:' + projectDir + '/'   // reads repos from this machine
 * //def repoRoot = 'http://192.168.1.77/'     // reads repos from another machine running hostFiles()
 * def assembleModel = coreModel.copy()
 * assembleModel.addRepo(repoRoot + P2_REPO_DIR)
 * ROOT_FEATURES.forEach() { feature ->
 *     assembleModel.addRepo('file:' + project.file(ROOT_FEATURE_DIR + feature))
 * }
 *
 * // assemble DiffPlug for each os
 * task assembleAll
 * def ASSEMBLE_TASK = 'assemble'
 * def assembleDir(OS os) { return project.file('build/10_assemble' + os + (os.isMac() ? ".app" : "")) }
 * INSTALLERS.each() { os ->
 *     def assembleOneTask = assembleModel.taskFor(project, ASSEMBLE_TASK + os, os, assembleDir(os))
 *     assembleOneTask.dependsOn(diffplugP2)
 *     assembleOneTask.dependsOn(checkRootFeatures)
 *     assembleAll.dependsOn(assembleOneTask)
 *
 *     // make the JRE executable if we can
 *     if (os.isMacOrLinux() && NATIVE_OS.isMacOrLinux()) {
 *         EclipsePlatform platform = EclipsePlatform.fromOS(os)
 *         assembleOneTask.doLast {
 *             def JRE_DIR = project.file(assembleDir(os).absolutePath + '/features/com.diffplug.jre.' + platform + '_' + VER_JRE + '/jre')
 *             CmdLine.runCmd(JRE_DIR, 'chmod -R a+x .')
 *         }
 *     }
 * }
 *
 * // test case which creates a DiffPlug from an existing DiffPlug
 * task reassembleAll
 * def REASSEMBLE_TASK = 'reassemble'
 * def reassembleDir(OS os) { return project.file('build/11_reassemble' + os + (os.isMac() ? ".app" : "")) }
 * INSTALLERS.each() { os ->
 *     def reassembleModel = coreModel.copy()
 *     def reassembleRoot = 'file:' + assembleDir(os)
 *     reassembleModel.addMetadataRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.engine/profileRegistry/ProfileDiffPlugP2.profile')
 *     reassembleModel.addArtifactRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.core/cache')
 *     reassembleModel.addArtifactRepo(reassembleRoot)
 *     def reassembleOneTask = reassembleModel.taskFor(project, REASSEMBLE_TASK + os, os, reassembleDir(os))
 *     reassembleOneTask.dependsOn(ASSEMBLE_TASK + os)
 *     reassembleAll.dependsOn(reassembleOneTask)
 * }
 * ```
 */
public class P2Model {
	/** Returns a deep copy of this model. */
	public P2Model copy() {
		P2Model copy = new P2Model();
		copy.ius.addAll(ius);
		copy.repos.addAll(repos);
		copy.metadataRepos.addAll(metadataRepos);
		copy.artifactRepos.addAll(artifactRepos);
		return copy;
	}

	private Set<String> ius = Sets.newHashSet();
	private Set<String> repos = Sets.newLinkedHashSet();
	private Set<String> metadataRepos = Sets.newLinkedHashSet();
	private Set<String> artifactRepos = Sets.newLinkedHashSet();

	/** Combines all fields for easy implementation of equals and hashCode. */
	private final List<Object> content = Arrays.asList(ius, repos, metadataRepos, artifactRepos);

	/** Hash of the models current content. */
	@Override
	public int hashCode() {
		return content.hashCode();
	}

	/** Two models are equal if all their fields are equal. */
	@Override
	public boolean equals(Object otherObj) {
		if (otherObj instanceof P2Model) {
			return content.equals(((P2Model) otherObj).content);
		} else {
			return false;
		}
	}

	public void addIU(String iu) {
		ius.add(iu);
	}

	public void addIU(String iu, String version) {
		ius.add(iu + "/" + version);
	}

	public void addFeature(String feature) {
		addIU(feature + ".feature.group");
	}

	public void addFeature(String feature, String version) {
		addIU(feature + ".feature.group", version);
	}

	public void addRepo(String repo) {
		repos.add(repo);
	}

	public void addRepo(File repo) {
		addRepo("file:" + repo.getAbsolutePath());
	}

	public void addMetadataRepo(String repo) {
		metadataRepos.add(repo);
	}

	public void addMetadataRepo(File repo) {
		addMetadataRepo("file:" + repo.getAbsolutePath());
	}

	public void addArtifactRepo(String repo) {
		artifactRepos.add(repo);
	}

	public void addArtifactRepo(File repo) {
		addArtifactRepo("file:" + repo.getAbsolutePath());
	}

	public void addArtifactRepoBundlePool() {
		addArtifactRepo(GoomphCacheLocations.bundlePool());
	}

	static final String FILE_PROTO = "file://";

	///////////////////////
	// P2 MIRROR via ANT //
	///////////////////////
	/**
	 * Creates a p2.mirror ant task file which will mirror the
	 * model's described IU's and repos into the given destination folder.
	 */
	@SuppressWarnings("unchecked")
	public String mirrorAntFile(File dstFolder) {
		Node p2mirror = new Node(null, "p2.mirror");
		sourceNode(p2mirror);
		Node destination = new Node(p2mirror, "destination");
		destination.attributes().put("location", FILE_PROTO + dstFolder.getAbsolutePath());

		for (String iu : ius) {
			Node iuNode = new Node(p2mirror, "iu");

			int slash = iu.indexOf('/');
			if (slash == -1) {
				iuNode.attributes().put("id", iu);
			} else {
				iuNode.attributes().put("id", iu.substring(0, slash));
				iuNode.attributes().put("version", iu.substring(slash + 1));
			}
		}
		return XmlUtil.serialize(p2mirror).replace("\r", "");
	}

	/** Creates an XML node representing all the repos in this model. */
	private Node sourceNode(Node parent) {
		Node source = new Node(parent, "source");
		@SuppressWarnings("unchecked")
		BiConsumer<Iterable<String>, Consumer<Map<String, String>>> addRepos = (urls, repoAttributes) -> {
			for (String url : urls) {
				Node repository = source.appendNode("repository");
				repository.attributes().put("location", url);
				repoAttributes.accept(repository.attributes());
			}
		};
		addRepos.accept(repos, Consumers.doNothing());
		addRepos.accept(metadataRepos, repoAttr -> repoAttr.put("kind", "metadata"));
		addRepos.accept(artifactRepos, repoAttr -> repoAttr.put("kind", "artifact"));
		return source;
	}

	public void mirror(File destination) {
		ant(mirrorAntFile(destination));
	}

	public void mirror(File destination, Action<? super XmlProvider> action) {
		ActionBroadcast<XmlProvider> xmlAction = new ActionBroadcast<XmlProvider>();
		xmlAction.add(action);
		XmlTransformer xmlTransformer = new XmlTransformer();
		xmlTransformer.addAction(action);
		ant(xmlTransformer.transform(mirrorAntFile(destination)));
	}

	private void ant(String buildfile) {
		// TODO: implement ant
	}

	public static final String ANT_DIRECTOR = "org.eclipse.ant.core.antRunner";

	public static class AntArgsBuilder extends EclipseArgsBuilder {
		public void define(String key, String value) {
			addArg("-D" + key + "=" + value);
		}
	}

	////////////////
	// P2DIRECTOR //
	////////////////
	/**
	 * Returns the arguments required to call "eclipsec" and run the p2 director application
	 * to install the artifacts from the repos in this model into the given directory and profile.
	 */
	public DirectorArgsBuilder directorArgs(File dstFolder, String profile) {
		DirectorArgsBuilder builder = new DirectorArgsBuilder();
		builder.clean();
		builder.consolelog();
		builder.application(P2_DIRECTOR);
		repos.forEach(repo -> builder.addArg("repository", repo));
		metadataRepos.forEach(repo -> builder.addArg("metadataRepository", repo));
		artifactRepos.forEach(repo -> builder.addArg("artifactRepository", repo));
		ius.forEach(iu -> builder.addArg("installIU", iu));
		builder.addArg("profile", profile);
		builder.addArg("destination", FILE_PROTO + dstFolder.getAbsolutePath());
		return builder;
	}

	public static final String P2_DIRECTOR = "org.eclipse.equinox.p2.director";

	/**
	 * An extension of EclipseArgsBuilder with typed methods appropriate for p2 director.
	 *
	 * Created using {@link P2Model#directorArgs(File, String)}.
	 */
	public static class DirectorArgsBuilder extends EclipseArgsBuilder {
		/**
		 * Adds a `bundlepool` argument.
		 *
		 * The location of where the plug-ins and features will be stored. This value
		 * is only taken into account when a new profile is created. For an application
		 * where all the bundles are located into the plugins/ folder of the destination,
		 * set it to `<destination>`.
		 */
		public void bundlepool(File bundlePool) {
			addArg("bundlepool", bundlePool.getAbsolutePath());
		}

		/** Adds `p2.os`, `p2.ws`, and `p2.arch` arguments. */
		public void oswsarch(SwtPlatform platform) {
			addArg("p2.os", platform.getOs());
			addArg("p2.ws", platform.getWs());
			addArg("p2.arch", platform.getArch());
		}

		/**
		 * Adds the `roaming` argument.
		 *
		 * Indicates that the product resulting from the installation can be moved.
		 * This property only makes sense when the destination and bundle pool are
		 * in the same location. This value is only taken into account when the
		 * profile is created.
		 */
		public void roaming() {
			addArg("roaming");
		}

		/**
		 * Adds the `shared` argument.
		 *
		 * use a shared location for the install. The path defaults to ${user.home}/.p2.
		 */
		public void shared() {
			addArg("shared");
		}

		/** @see #shared() */
		public void shared(File shared) {
			addArg("shared", shared.getAbsolutePath());
		}
	}

	/** Deletes the cached repository info (which may include references to local paths). */
	public static void cleanCachedRepositories(File dstFile) throws IOException {
		Path path = dstFile.toPath().resolve("p2/org.eclipse.equinox.p2.engine/.settings");
		FileMisc.cleanDir(path.toFile());
	}

	/** See {@link #install(File, String, Consumer)}. */
	public void install(File dstFolder, String profile) throws Throwable {
		install(dstFolder, profile, Actions.doNothing());
	}

	/**
	 * Installs the IUs from the repos specified in this object into the given folder.
	 *
	 * This uses the P2 director application from the latest available 
	 *
	 * For more args, see [the p2 director docs](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html&cp=2_0_20_2).
	 *
	 * In particular, you might want to add "roaming", if you'd like to copy-paste an installation around.
	 *
	 * @param dstFolder the folder into which the installation will take place.
	 * @param profile the name of the profile, doesn't really matter what it is.
	 */
	public void install(File dstFolder, String profile, Action<DirectorArgsBuilder> configModify) throws Exception {
		// setup the args
		DirectorArgsBuilder args = directorArgs(dstFolder, profile);
		configModify.execute(args);
		// ensure the bootstrap installation is installed
		P2BootstrapInstallation installation = P2BootstrapInstallation.latest();
		installation.ensureInstalled();
		// launch the equinox application with these arguments
		RunOutside runOutside = new RunOutside(installation.getRootFolder(), args.toArgList());
		Errors.constrainTo(Exception.class).run(() -> JavaExecable.execWithoutGradle(runOutside));
	}

	/**
	 * If you run two P2 tasks back-to-back, you get the exception dumped below.
	 * P2's OSGi requirement is such a nightmare.
	 *
	 * Easiest fix is to run it outside of this JVM.
	 */
	@SuppressWarnings("serial")
	static class RunOutside implements JavaExecable {
		final File rootFolder;
		final List<String> args;

		public RunOutside(File rootFolder, List<String> args) {
			this.rootFolder = rootFolder;
			this.args = args;
		}

		@Override
		public void run() throws Throwable {
			EquinoxLauncher launcher = new EquinoxLauncher(rootFolder);
			launcher.setArgs(args);
			launcher.run();
		}
	}

	/* Exception if you run two P2 tasks back to back.
	!SESSION 2016-06-16 15:52:15.882 -----------------------------------------------
	eclipse.buildId=unknown
	java.version=1.8.0_74
	java.vendor=Oracle Corporation
	BootLoader constants: OS=win32, ARCH=x86_64, WS=win32, NL=en_US
	Framework arguments:  -application org.eclipse.equinox.p2.director -repository http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/ -artifactRepository file:C:\Users\ntwigg\.goomph\shared-bundles -installIU org.eclipse.rcp.configuration.feature.group,org.eclipse.equinox.executable.feature.group -profile profile -destination file:C:\Users\ntwigg\Documents\DiffPlugDev\DiffPlug\targetplatform\build\goomph-p2asmaven\__p2__ -profileProperties org.eclipse.update.install.features=true -p2.os win32 -p2.ws win32 -p2.arch x86
	Command-line arguments:  -clean -consolelog -application org.eclipse.equinox.p2.director -repository http://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/ -artifactRepository file:C:\Users\ntwigg\.goomph\shared-bundles -installIU org.eclipse.rcp.configuration.feature.group,org.eclipse.equinox.executable.feature.group -profile profile -destination file:C:\Users\ntwigg\Documents\DiffPlugDev\DiffPlug\targetplatform\build\goomph-p2asmaven\__p2__ -profileProperties org.eclipse.update.install.features=true -p2.os win32 -p2.ws win32 -p2.arch x86
	
	!ENTRY org.eclipse.update.configurator 4 0 2016-06-16 15:52:15.886
	!MESSAGE FrameworkEvent ERROR
	!STACK 0
	org.osgi.framework.BundleException: Exception in org.eclipse.update.internal.configurator.ConfigurationActivator.start() of bundle org.eclipse.update.configurator.
		at org.eclipse.osgi.internal.framework.BundleContextImpl.startActivator(BundleContextImpl.java:792)
		at org.eclipse.osgi.internal.framework.BundleContextImpl.start(BundleContextImpl.java:721)
		at org.eclipse.osgi.internal.framework.EquinoxBundle.startWorker0(EquinoxBundle.java:941)
		at org.eclipse.osgi.internal.framework.EquinoxBundle$EquinoxModule.startWorker(EquinoxBundle.java:318)
		at org.eclipse.osgi.container.Module.doStart(Module.java:571)
		at org.eclipse.osgi.container.Module.start(Module.java:439)
		at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.incStartLevel(ModuleContainer.java:1582)
		at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.incStartLevel(ModuleContainer.java:1562)
		at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.doContainerStartLevel(ModuleContainer.java:1533)
		at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.dispatchEvent(ModuleContainer.java:1476)
		at org.eclipse.osgi.container.ModuleContainer$ContainerStartLevel.dispatchEvent(ModuleContainer.java:1)
		at org.eclipse.osgi.framework.eventmgr.EventManager.dispatchEvent(EventManager.java:230)
		at org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.run(EventManager.java:340)
	Caused by: javax.xml.parsers.FactoryConfigurationError: Provider for class javax.xml.parsers.SAXParserFactory cannot be created
		at javax.xml.parsers.FactoryFinder.findServiceProvider(FactoryFinder.java:311)
		at javax.xml.parsers.FactoryFinder.find(FactoryFinder.java:267)
		at javax.xml.parsers.SAXParserFactory.newInstance(SAXParserFactory.java:127)
		at org.eclipse.update.internal.configurator.ConfigurationParser.<clinit>(ConfigurationParser.java:34)
		at org.eclipse.update.internal.configurator.PlatformConfiguration.loadConfig(PlatformConfiguration.java:1081)
		at org.eclipse.update.internal.configurator.PlatformConfiguration.initializeCurrent(PlatformConfiguration.java:752)
		at org.eclipse.update.internal.configurator.PlatformConfiguration.<init>(PlatformConfiguration.java:104)
		at org.eclipse.update.internal.configurator.PlatformConfiguration.startup(PlatformConfiguration.java:707)
		at org.eclipse.update.internal.configurator.ConfigurationActivator.getPlatformConfiguration(ConfigurationActivator.java:404)
		at org.eclipse.update.internal.configurator.ConfigurationActivator.initialize(ConfigurationActivator.java:136)
		at org.eclipse.update.internal.configurator.ConfigurationActivator.start(ConfigurationActivator.java:69)
		at org.eclipse.osgi.internal.framework.BundleContextImpl$3.run(BundleContextImpl.java:771)
		at org.eclipse.osgi.internal.framework.BundleContextImpl$3.run(BundleContextImpl.java:1)
		at java.security.AccessController.doPrivileged(Native Method)
		at org.eclipse.osgi.internal.framework.BundleContextImpl.startActivator(BundleContextImpl.java:764)
		... 12 more
	Caused by: java.lang.RuntimeException: Provider for class javax.xml.parsers.SAXParserFactory cannot be created
		at javax.xml.parsers.FactoryFinder.findServiceProvider(FactoryFinder.java:308)
		... 26 more
	Caused by: java.util.ServiceConfigurationError: javax.xml.parsers.SAXParserFactory: Provider org.apache.xerces.jaxp.SAXParserFactoryImpl not found
		at java.util.ServiceLoader.fail(ServiceLoader.java:239)
		at java.util.ServiceLoader.access$300(ServiceLoader.java:185)
		at java.util.ServiceLoader$LazyIterator.nextService(ServiceLoader.java:372)
		at java.util.ServiceLoader$LazyIterator.next(ServiceLoader.java:404)
		at java.util.ServiceLoader$1.next(ServiceLoader.java:480)
		at javax.xml.parsers.FactoryFinder$1.run(FactoryFinder.java:294)
		at java.security.AccessController.doPrivileged(Native Method)
		at javax.xml.parsers.FactoryFinder.findServiceProvider(FactoryFinder.java:289)
		... 26 more
		*/
}
