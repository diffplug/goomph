/*
 * Copyright (C) 2016-2019 DiffPlug
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


import com.diffplug.common.collect.HashMultimap;
import com.diffplug.common.collect.Multimap;
import com.diffplug.gradle.FileMisc;
import groovy.util.Node;
import groovy.xml.XmlUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.osgi.framework.Version;

/** Builds a maven repo out of a p2 repository. */
class MavenRepoBuilder implements AutoCloseable {
	final File root;
	final Multimap<Coordinate, Artifact> artifactMap = HashMultimap.create();

	MavenRepoBuilder(File root) throws Exception {
		this.root = Objects.requireNonNull(root);
	}

	/**
	 * Installs the given OSGi jar into the given group.
	 *
	 * Parses the name from Bundle-SymbolicName, the version
	 * from Bundle-Version, and the source for Eclipse-SourceBundle.
	 */
	public void install(String group, File osgiJar) throws Exception {
		ParsedJar parsed = ParsedJar.parse(osgiJar);
		artifactMap.put(new Coordinate(group, parsed.getSymbolicName()),
				new Artifact(Version.parseVersion(parsed.getVersion()), parsed.isSource(), osgiJar));
	}

	@Override
	public void close() throws Exception {
		for (Coordinate coord : artifactMap.keySet()) {
			File groupFolder = new File(root, coord.group);
			File artifactFolder = new File(groupFolder, coord.artifactId);
			FileMisc.mkdirs(artifactFolder);
			Collection<Artifact> values = artifactMap.get(coord);
			install(artifactFolder, coord, values);
		}
	}

	private void install(File artifactFolder, Coordinate coord, Collection<Artifact> artifacts) throws IOException {
		List<Version> allVersions = artifacts.stream()
				.map(artifact -> artifact.version)
				.distinct().sorted().collect(Collectors.toList());
		// create the metadata
		Node metadata = new Node(null, "metadata");
		new Node(metadata, "groupId").setValue(coord.group);
		new Node(metadata, "artifactId").setValue(coord.artifactId);
		// the last one
		new Node(metadata, "version").setValue(allVersions.get(allVersions.size() - 1));
		Node versioning = new Node(metadata, "versioning");
		Node versions = new Node(versioning, "versions");
		for (Version version : allVersions) {
			new Node(versions, "version").setValue(version.toString());
		}
		new Node(versioning, "lastUpdated").setValue(System.currentTimeMillis());
		// create the metadata file
		String mavenMetadataContent = FileMisc.toUnixNewline(XmlUtil.serialize(metadata));
		File mavenMetadata = new File(artifactFolder, "maven-metadata.xml");
		Files.write(mavenMetadata.toPath(), mavenMetadataContent.getBytes(StandardCharsets.UTF_8));
		// write out the artifacts
		for (Artifact artifact : artifacts) {
			StringBuilder builder = new StringBuilder();
			builder.append(coord.artifactId);
			builder.append('-');
			builder.append(artifact.version.toString());
			if (artifact.isSources) {
				builder.append("-sources");
			}
			builder.append(".jar");
			File versionFolder = new File(artifactFolder, artifact.version.toString());
			FileMisc.mkdirs(versionFolder);
			Files.copy(artifact.jar.toPath(), new File(versionFolder, builder.toString()).toPath());
		}
	}

	static class Coordinate {
		final String group;
		final String artifactId;

		public Coordinate(String group, String artifactId) {
			this.group = group;
			this.artifactId = artifactId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(group, artifactId);
		}

		@Override
		public boolean equals(Object otherObj) {
			if (otherObj instanceof Coordinate) {
				Coordinate other = (Coordinate) otherObj;
				return other.group.equals(group) && other.artifactId.equals(artifactId);
			} else {
				return false;
			}
		}
	}

	static class Artifact implements Comparable<Artifact> {
		final Version version;
		final boolean isSources;
		final File jar;

		public Artifact(Version version, boolean isSources, File jar) {
			this.version = Objects.requireNonNull(version);
			this.isSources = isSources;
			this.jar = Objects.requireNonNull(jar);
		}

		// Comparison and equality based on version and classifier, but not jar
		static final Comparator<Artifact> comparator;
		static {
			Comparator<Artifact> byVersion = Comparator.comparing(artifact -> artifact.version);
			comparator = byVersion.thenComparing(artifact -> artifact.isSources ? 0 : 1);
		}

		@Override
		public int compareTo(Artifact other) {
			return comparator.compare(this, other);
		}

		@Override
		public boolean equals(Object otherObj) {
			if (otherObj instanceof Artifact) {
				Artifact other = (Artifact) otherObj;
				return comparator.compare(this, other) == 0;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(version, isSources);
		}
	}
}
