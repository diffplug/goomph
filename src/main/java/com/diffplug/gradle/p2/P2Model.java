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

import groovy.util.Node;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.collect.Sets;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.eclipserunner.EclipseApp;

/**
 * Models a request for some artifacts from some
 * p2 repositories.
 * 
 * Install with p2 director using {@link #director(File, String, Action)}
 * 
 * Mirror with the ant p2 mirror task using {@link #mirror(File, Action)}.
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
	public MirrorApp mirrorApp(File dstFolder) {
		MirrorApp ant = new MirrorApp();

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
		ant.setTask(p2mirror);
		return ant;
	}

	/** @see #mirrorApp(File) */
	public static class MirrorApp extends EclipseApp.AntRunner {
		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper() throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
		}
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

	////////////////
	// P2DIRECTOR //
	////////////////
	/**
	 * Returns the arguments required to call "eclipsec" and run the p2 director application
	 * to install the artifacts from the repos in this model into the given directory and profile.
	 */
	public DirectorApp directorApp(File dstFolder, String profile) {
		DirectorApp builder = new DirectorApp();
		builder.clean();
		builder.consolelog();
		repos.forEach(repo -> builder.addArg("repository", repo));
		metadataRepos.forEach(repo -> builder.addArg("metadataRepository", repo));
		artifactRepos.forEach(repo -> builder.addArg("artifactRepository", repo));
		ius.forEach(iu -> builder.addArg("installIU", iu));
		builder.addArg("profile", profile);
		builder.addArg("destination", FILE_PROTO + dstFolder.getAbsolutePath());
		return builder;
	}

	/**
	 * An extension of EclipseApp with typed methods appropriate for p2 director.
	 *
	 * Created using {@link P2Model#directorApp(File, String)}.
	 */
	public static class DirectorApp extends EclipseApp {
		public DirectorApp() {
			super("org.eclipse.equinox.p2.director");
		}

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

		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper() throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
		}
	}

	/** Deletes the cached repository info (which may include references to local paths). */
	public static void cleanCachedRepositories(File dstFile) throws IOException {
		Path path = dstFile.toPath().resolve("p2/org.eclipse.equinox.p2.engine/.settings");
		FileMisc.cleanDir(path.toFile());
	}
}
