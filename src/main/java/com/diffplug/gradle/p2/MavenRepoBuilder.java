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
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;

import com.google.inject.AbstractModule;

/** Builds a maven repo out of a p2 repository. */
class MavenRepoBuilder implements AutoCloseable {
	final DefaultPlexusContainer container;
	final MavenArtifactRepository repo;
	final ArtifactInstaller installer;

	MavenRepoBuilder(File root) throws Exception {
		ClassWorld classWorld = new ClassWorld("plexus.core", MavenRepoBuilder.class.getClassLoader());
		ContainerConfiguration cc = new DefaultContainerConfiguration()
				.setClassWorld(classWorld)
				.setClassPathScanning(PlexusConstants.SCANNING_CACHE)
				.setAutoWiring(true)
				.setName("maven");
		container = new DefaultPlexusContainer(cc, new AbstractModule() {
			@Override
			protected void configure() {
				bind(ArtifactInstaller.class).to(DpArtifactInstaller.class);
			}
		});
		installer = container.lookup(ArtifactInstaller.class);
		ArtifactRepositoryPolicy policy = new ArtifactRepositoryPolicy();
		repo = new MavenArtifactRepository("id", "file:" + root.getAbsolutePath(), new DefaultRepositoryLayout(), policy, policy);
	}

	@Override
	public void close() {
		container.dispose();
	}

	@Component(role = ArtifactInstaller.class)
	static class DpArtifactInstaller extends AbstractLogEnabled implements ArtifactInstaller {
		@Requirement
		private RepositorySystem repoSystem;

		@Deprecated
		public void install(String basedir, String finalName, Artifact artifact, ArtifactRepository localRepository) throws ArtifactInstallationException {
			throw new UnsupportedOperationException();
		}

		public void install(File source, Artifact artifact, ArtifactRepository localRepository) throws ArtifactInstallationException {
			RepositorySystemSession session = LegacyLocalRepositoryManager.overlay(localRepository, null, repoSystem);
			InstallRequest request = new InstallRequest();

			org.eclipse.aether.artifact.Artifact mainArtifact = RepositoryUtils.toArtifact(artifact);
			mainArtifact = mainArtifact.setFile(source);
			request.addArtifact(mainArtifact);
			try {
				repoSystem.install(session, request);
			} catch (InstallationException e) {
				throw new ArtifactInstallationException(e.getMessage(), e);
			}
			Versioning versioning = new Versioning();
			versioning.updateTimestamp();
			versioning.addVersion(artifact.getBaseVersion());
			versioning.setRelease(artifact.getBaseVersion());
			artifact.addMetadata(new ArtifactRepositoryMetadata(artifact, versioning));
		}
	}

	/**
	 * Installs the given OSGi jar into the given group.
	 *
	 * Parses the name from Bundle-SymbolicName, the version
	 * from Bundle-Version, and the source for Eclipse-SourceBundle.
	 */
	public void install(String group, File osgiJar) throws Exception {
		String symbolicName;
		String version;
		boolean isSource;
		try (JarFile jarFile = new JarFile(osgiJar)) {
			Attributes attr = jarFile.getManifest().getMainAttributes();
			symbolicName = beforeSemicolon(attr.getValue("Bundle-SymbolicName"));
			version = attr.getValue("Bundle-Version");
			String source = attr.getValue("Eclipse-SourceBundle");
			if (source != null) {
				isSource = true;
				symbolicName = beforeSemicolon(source);
			} else {
				isSource = false;
			}
		}
		String scope = "compile";
		String type = "jar";
		String classifier = isSource ? "sources" : "";
		ArtifactHandler handler = new DefaultArtifactHandler("jar");
		Artifact artifact = new DefaultArtifact(group, symbolicName, version, scope, type, classifier, handler);
		installer.install(osgiJar, artifact, repo);
	}

	/** Parses out a name from an OSGi manifest header. */
	private static String beforeSemicolon(String input) {
		int firstSemiColon = input.indexOf(';');
		if (firstSemiColon == -1) {
			return input;
		} else {
			return input.substring(0, firstSemiColon);
		}
	}
}
