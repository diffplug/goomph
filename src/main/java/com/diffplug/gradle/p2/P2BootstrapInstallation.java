/*
 * Copyright 2020 DiffPlug
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


import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.ImmutableSet;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ZipMisc;
import com.diffplug.gradle.eclipserunner.EclipseRunner;
import com.diffplug.gradle.eclipserunner.JarFolderRunner;
import com.diffplug.gradle.eclipserunner.JarFolderRunnerExternalJvm;
import com.diffplug.gradle.pde.EclipseRelease;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import org.gradle.api.Project;

/** Wraps a Bootstrap installation for the given eclipse release. */
class P2BootstrapInstallation {
	static final String DOWNLOAD_ROOT = "https://dl.bintray.com/diffplug/opensource/com/diffplug/gradle/goomph-p2-bootstrap/";
	static final String DOWNLOAD_FILE = "/goomph-p2-bootstrap.zip";
	static final String VERSIONED_DOWNLOAD_FILE = "/goomph-p2-bootstrap-%s.zip";

	/** List of versions for which we have deployed a bootstrap to bintray. */
	static final ImmutableSet<EclipseRelease> SUPPORTED = ImmutableSet.of(
			EclipseRelease.official("4.5.2"),
			EclipseRelease.official("4.7.2"));

	final EclipseRelease release;

	static P2BootstrapInstallation latest() {
		EclipseRelease latest = SUPPORTED.asList().listIterator(SUPPORTED.size()).previous();
		return new P2BootstrapInstallation(latest);
	}

	P2BootstrapInstallation(EclipseRelease release) {
		this.release = Objects.requireNonNull(release);
		// install() will only work for officially supported versions
		Preconditions.checkArgument(SUPPORTED.contains(release), "We only have bootstrap for ", SUPPORTED);
	}

	/** The root of this installation. */
	File getRootFolder() {
		return new File(GoomphCacheLocations.p2bootstrap(), release.toString());
	}

	/** Makes sure that the installation is prepared. */
	void ensureInstalled() throws IOException {
		if (!isInstalled()) {
			install();
		}
	}

	static final String TOKEN = "installed";

	/** Returns true iff it is installed. */
	private boolean isInstalled() throws IOException {
		return FileMisc.hasToken(getRootFolder(), TOKEN);
	}

	/** Installs the bootstrap installation. */
	private void install() throws IOException {
		System.out.print("Installing p2 bootstrap " + release + "... ");
		// clean the install folder
		FileMisc.cleanDir(getRootFolder());
		// download the URL
		File target = new File(getRootFolder(), DOWNLOAD_FILE);
		try {
			FileMisc.download(GoomphCacheLocations.p2bootstrapUrl().orElse(DOWNLOAD_ROOT) + release.version() + DOWNLOAD_FILE, target);
		} catch (FileNotFoundException ex) {
			//try versioned artifact - Common when boostrap is on a maven type(sonatype nexus, etc.) repository.
			FileMisc.download(GoomphCacheLocations.p2bootstrapUrl().orElse(DOWNLOAD_ROOT) + release.version() + String.format(VERSIONED_DOWNLOAD_FILE, release.version()), target);
		}
		// unzip it
		ZipMisc.unzip(target, target.getParentFile());
		// delete the zip
		FileMisc.forceDelete(target);
		FileMisc.writeToken(getRootFolder(), TOKEN);
		System.out.print("Success.");
	}

	/**
	 * Creates a model containing p2-director for the given {@link EclipseRelease}.
	 *
	 * Useful for updating [bintray](https://bintray.com/diffplug/opensource/goomph-p2-bootstrap/view).
	 */
	P2Model p2model() {
		P2Model model = new P2Model();
		// the update site for the release we're downloading artifacts for
		model.addRepo(release.updateSite());
		// core p2 features
		model.addFeature("org.eclipse.equinox.p2.core.feature");
		model.addIU("org.eclipse.equinox.p2.director.app"); // p2 director
		model.addIU("org.eclipse.equinox.p2.repository.tools"); // p2 repository mirror
		// failed transitive required for basic p2 operations
		model.addIU("org.eclipse.core.net");
		// failed transitive required for shared installations touchpoints
		model.addIU("org.eclipse.osgi.compatibility.state");
		// ant tasks
		model.addIU("org.eclipse.ant.core");
		model.addIU("org.apache.ant");
		// eclipse infrastructure to make "eclipsec -application org.eclipse.equinox.p2.director" work
		model.addIU("org.eclipse.core.runtime");
		model.addIU("org.eclipse.update.configurator");
		model.addIU("org.eclipse.equinox.ds");
		return model;
	}

	/** Returns an EclipseArgsBuilder.Runner which runs within this JVM. */
	public EclipseRunner withinJvmRunner() throws IOException {
		return args -> {
			ensureInstalled();
			new JarFolderRunner(getRootFolder()).run(args);
		};
	}

	/** Returns an EclipseArgsBuilder.Runner which runs outside this JVM. */
	public EclipseRunner outsideJvmRunner() throws IOException {
		return args -> {
			ensureInstalled();
			new JarFolderRunnerExternalJvm(getRootFolder()).run(args);
		};
	}

	/** Returns an EclipseArgsBuilder.Runner which runs outside this JVM. */
	public EclipseRunner outsideJvmRunner(Project project) throws IOException {
		return args -> {
			ensureInstalled();
			new JarFolderRunnerExternalJvm(getRootFolder(), project).run(args);
		};
	}

	/* Exception if you run two P2 tasks back to back.
	!SESSION 2016-06-16 15:52:15.882 -----------------------------------------------
	eclipse.buildId=unknown
	java.version=1.8.0_74
	java.vendor=Oracle Corporation
	BootLoader constants: OS=win32, ARCH=x86_64, WS=win32, NL=en_US
	Framework arguments:  -application org.eclipse.equinox.p2.director -repository https://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/ -artifactRepository file:C:\Users\ntwigg\.goomph\shared-bundles -installIU org.eclipse.rcp.configuration.feature.group,org.eclipse.equinox.executable.feature.group -profile profile -destination file:C:\Users\ntwigg\Documents\DiffPlugDev\DiffPlug\targetplatform\build\p2asmaven\__p2__ -profileProperties org.eclipse.update.install.features=true -p2.os win32 -p2.ws win32 -p2.arch x86
	Command-line arguments:  -clean -consolelog -application org.eclipse.equinox.p2.director -repository https://download.eclipse.org/eclipse/updates/4.5/R-4.5.2-201602121500/ -artifactRepository file:C:\Users\ntwigg\.goomph\shared-bundles -installIU org.eclipse.rcp.configuration.feature.group,org.eclipse.equinox.executable.feature.group -profile profile -destination file:C:\Users\ntwigg\Documents\DiffPlugDev\DiffPlug\targetplatform\build\p2asmaven\__p2__ -profileProperties org.eclipse.update.install.features=true -p2.os win32 -p2.ws win32 -p2.arch x86
	
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
