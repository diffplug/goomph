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
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;

import groovy.util.Node;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.eclipserunner.EclipseApp;
import com.diffplug.gradle.eclipserunner.EclipseRunner;
import com.diffplug.gradle.pde.EclipseRelease;

/**
 * Models a request for some artifacts from some
 * p2 repositories.
 * 
 * - Install with p2 director using {@link #directorApp(File, String)}.
 * - Mirror with the ant p2 mirror task using {@link #mirrorApp(File)}.
 */
public class P2Model implements Serializable {
	private static final long serialVersionUID = 6458767795698285906L;

	/** Returns a deep copy of this model. */
	public P2Model copy() {
		P2Model copy = new P2Model();
		copy.copyFrom(this);
		return copy;
	}

	/** Copies everything from the other model into this one. */
	public void copyFrom(P2Model other) {
		ius.addAll(other.ius);
		repos.addAll(other.repos);
		metadataRepos.addAll(other.metadataRepos);
		artifactRepos.addAll(other.artifactRepos);
	}

	private Set<String> ius = new LinkedHashSet<>();
	private Set<String> repos = new LinkedHashSet<>();
	private Set<String> metadataRepos = new LinkedHashSet<>();
	private Set<String> artifactRepos = new LinkedHashSet<>();

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

	@Override
	public String toString() {
		return StringPrinter.buildString(printer -> {
			BiConsumer<String, Set<String>> add = (name, set) -> {
				for (String element : set) {
					printer.print(name);
					printer.print(": ");
					printer.print(element);
					printer.println("");
				}
			};
			add.accept("repo", repos);
			add.accept("metadataRepo", metadataRepos);
			add.accept("artifactRepo", artifactRepos);
			add.accept("ius", ius);
		});
	}

	public void addIU(String iu) {
		ius.add(iu);
	}

	public void addIU(String iu, String version) {
		ius.add(iu + "/" + version);
	}

	private static final String FEATURE_GROUP = ".feature.group";

	public void addFeature(String feature) {
		addIU(feature + FEATURE_GROUP);
	}

	public void addFeature(String feature, String version) {
		addIU(feature + FEATURE_GROUP, version);
	}

	public Set<String> getRepos() {
		return repos;
	}

	public void addRepoEclipse(String release) {
		addRepo(EclipseRelease.official(release).updateSite());
	}

	public void addRepo(String repo) {
		repos.add(repo);
	}

	public void addRepo(File repo) {
		addRepo(FileMisc.PROTOCOL + repo.getAbsolutePath());
	}

	public void addMetadataRepo(String repo) {
		metadataRepos.add(repo);
	}

	public void addMetadataRepo(File repo) {
		addMetadataRepo(FileMisc.PROTOCOL + repo.getAbsolutePath());
	}

	public void addArtifactRepo(String repo) {
		artifactRepos.add(repo);
	}

	public void addArtifactRepo(File repo) {
		addArtifactRepo(FileMisc.PROTOCOL + repo.getAbsolutePath());
	}

	public void addArtifactRepoBundlePool() {
		addArtifactRepo(GoomphCacheLocations.bundlePool());
	}

	/**
	 * Removes all missing local repositories (to fix "No repository found at" warnings).
	 * 
	 * Returns true iff any repos were removed.
	 */
	private boolean removeNonExistentLocal() {
		// predicate which matches local repositories which don't exist
		Predicate<String> nonExistentLocal = repo -> {
			if (!repo.startsWith(FileMisc.PROTOCOL)) {
				// it's not local
				return false;
			}
			File file = new File(repo.substring(FileMisc.PROTOCOL.length()));
			if (!file.isDirectory()) {
				// it is local and there's no directory, we match
				return true;
			} else {
				// if there's an artifacts.xml/jar or a content.xml/jar, then it's not a match
				for (File child : FileMisc.list(file)) {
					if (child.isFile()) {
						if (child.getName().startsWith("artifacts.") || child.getName().startsWith("content.")) {
							return false;
						}
					}
				}
				// else, it is a non-existent local
				return true;
			}
		};
		// remove unreachable local repositories
		boolean anyRemoved = false;
		anyRemoved |= repos.removeIf(nonExistentLocal);
		anyRemoved |= metadataRepos.removeIf(nonExistentLocal);
		anyRemoved |= artifactRepos.removeIf(nonExistentLocal);
		return anyRemoved;
	}

	/**
	 * So there are places where we add the local bundle pool
	 * to act as an artifact cache.  But sometimes, that cache
	 * doesn't exist yet.  To make sure we don't get an error
	 * just because a cache doesn't exist, we take the following
	 * action:
	 * 
	 * - Copy the current model.
	 * - Remove all local repos which don't exist
	 * - Call the supplier.
	 * - If removed any repos, put them back.
	 * - Return the result.
	 */
	private <T> T performWithoutNonExistentLocalRepos(Supplier<T> supplier) {
		P2Model backup = copy();
		boolean needToRestore = removeNonExistentLocal();
		T result = supplier.get();
		if (needToRestore) {
			repos.addAll(backup.repos);
			metadataRepos.addAll(backup.metadataRepos);
			artifactRepos.addAll(backup.artifactRepos);
		}
		return result;
	}

	///////////////////////
	// P2 MIRROR via ANT //
	///////////////////////
	/**
	 * Creates a p2.mirror ant task file which will mirror the
	 * model's described IU's and repos into the given destination folder.
	 * 
	 * @see MirrorApp
	 */
	@SuppressWarnings("unchecked")
	public MirrorApp mirrorApp(File dstFolder) {
		return performWithoutNonExistentLocalRepos(() -> {
			MirrorApp ant = new MirrorApp();

			Node p2mirror = new Node(null, "p2.mirror");
			sourceNode(p2mirror);
			Node destination = new Node(p2mirror, "destination");
			destination.attributes().put("location", FileMisc.PROTOCOL + dstFolder.getAbsolutePath());

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
		});
	}

	/**
	 * An extension of {@link EclipseApp.AntRunner} which is
	 * pre-populated with an ant file appropriate for running
	 * the p2.mirror ant task.
	 *
	 * Created using {@link P2Model#mirrorApp(File)}.
	 */
	public static class MirrorApp extends EclipseApp.AntRunner {
		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper() throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
		}

		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper(Project project) throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner(project));
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
		return performWithoutNonExistentLocalRepos(() -> {
			DirectorApp builder = new DirectorApp();
			builder.clean();
			builder.consolelog();
			repos.forEach(repo -> builder.addArg("repository", repo));
			metadataRepos.forEach(repo -> builder.addArg("metadataRepository", repo));
			artifactRepos.forEach(repo -> builder.addArg("artifactRepository", repo));
			ius.forEach(iu -> builder.addArg("installIU", iu));
			builder.addArg("profile", profile);
			builder.addArg("destination", FileMisc.PROTOCOL + dstFolder.getAbsolutePath());
			// deletes cached repository information, which will often include local paths
			builder.doLast.add(() -> {
				Path path = dstFolder.toPath().resolve("p2/org.eclipse.equinox.p2.engine/.settings");
				FileUtils.deleteDirectory(path.toFile());
			});
			return builder;
		});
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
		public void platform(SwtPlatform platform) {
			addArg("p2.os", platform.getOs());
			addArg("p2.ws", platform.getWs());
			addArg("p2.arch", platform.getArch());
			if (platform.getOs().equals("macosx")) {
				String dest = args.get("destination").get(0);
				if (!dest.endsWith(".app")) {
					System.err.println("WARNING: Mac installs should end with '.app', this is " + dest);
				}
			}
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

		/** Adds a profile property. */
		public void profileProperty(String key, String value) {
			addArg("profileProperties", FileMisc.noQuote(key) + "=" + FileMisc.noQuote(value));
		}

		/** Sets a profile property to ensure that features are installed. */
		public void installFeatures() {
			profileProperty("org.eclipse.update.install.features", "true");
		}

		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper() throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
		}

		/** Runs this application, downloading a small bootstrapper if necessary. */
		public void runUsingBootstrapper(Project project) throws Exception {
			runUsing(P2BootstrapInstallation.latest().outsideJvmRunner(project));
		}

		final List<Throwing.Runnable> doLast = new ArrayList<>();

		@Override
		public void runUsing(EclipseRunner runner) throws Exception {
			super.runUsing(runner);
			for (Throwing.Runnable toRun : doLast) {
				Errors.constrainTo(Exception.class).run(toRun);
			}
		}
	}
}
