/*
 * Copyright (C) 2018-2021 DiffPlug
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
package com.diffplug.gradle.eclipse;


import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ZipMisc;
import com.diffplug.gradle.pde.EclipseRelease;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** Maps eclipse jars to their mavenCentral artifact ids and versions based on their official release. */
public class MavenCentralMapping {
	private static final EclipseRelease FIRST_ON_CENTRAL = EclipseRelease.official("4.6.2");

	private static final String PLATFORM = "org.eclipse.platform";
	private static final String JDT = "org.eclipse.jdt";
	private static final String PDE = "org.eclipse.pde";
	private static final String EMF = "org.eclipse.emf";
	private static final String ECF = "org.eclipse.ecf";
	private static final String OSGI = "org.osgi";

	private static final String ICU_BUNDLE_ID = "com.ibm.icu";

	public static boolean isEclipseGroup(String group) {
		return group.equals(PLATFORM) || group.equals(JDT) || group.equals(PDE) || group.equals(EMF) || group.equals(ECF);
	}

	/** Returns the MavenCentral groupId:artifactId appropriate for the given bundleId. */
	public static String groupIdArtifactId(String bundleId) {
		if (ICU_BUNDLE_ID.equals(bundleId)) {
			return "com.ibm.icu:icu4j";
		} else if ("org.eclipse.jdt.core.compiler.batch".equals(bundleId)) {
			return JDT + ":ecj";
		} else if (bundleId.startsWith(JDT)) {
			return JDT + ":" + bundleId;
		} else if (bundleId.startsWith(PDE)) {
			return PDE + ":" + bundleId;
		} else if (bundleId.startsWith(EMF)) {
			return EMF + ":" + bundleId;
		} else if (bundleId.startsWith(ECF)) {
			return ECF + ":" + bundleId;
		} else if (bundleId.startsWith(OSGI)) {
			return OSGI + ":" + bundleId;
		} else {
			return PLATFORM + ":" + bundleId;
		}
	}

	/** Creates a map from a key defined by the keyExtractor function to its corresponding version in maven central. */
	static Map<String, String> parse(InputStream inputStream, Function<String, String> keyExtractor) throws ParserConfigurationException, SAXException, IOException {
		Map<String, String> map = new HashMap<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream);
		Node artifacts = doc.getDocumentElement().getElementsByTagName("artifacts").item(0);
		for (int i = 0; i < artifacts.getChildNodes().getLength(); ++i) {
			Node artifact = artifacts.getChildNodes().item(i);
			if ("artifact".equals(artifact.getNodeName())) {
				String classifier = artifact.getAttributes().getNamedItem("classifier").getNodeValue();
				if ("osgi.bundle".equals(classifier)) {
					String bundleId = artifact.getAttributes().getNamedItem("id").getNodeValue();
					String bundleVersion = artifact.getAttributes().getNamedItem("version").getNodeValue();
					String key = keyExtractor.apply(bundleId);
					String version = calculateMavenCentralVersion(bundleId, bundleVersion);
					map.put(key, version);
				}
			}
		}
		return map;
	}

	static String calculateMavenCentralVersion(String bundleId, String bundleVersion) {
		if ("org.eclipse.equinox.preferences".equals(bundleId) && "3.10.0.v20220503-1634".equals(bundleVersion)) {
			// See https://github.com/eclipse-equinox/equinox.bundles/issues/54
			return "3.10.1";
		} else if ("org.eclipse.osgi.util".equals(bundleId) && "3.7.0.v20220427-2144".equals(bundleVersion)) {
			// See https://github.com/eclipse-equinox/equinox.framework/issues/70
			return "3.7.1";
		}
		Version parsed = Version.parseVersion(bundleVersion);
		if (ICU_BUNDLE_ID.equals(bundleId) && parsed.getMicro() == 0) {
			return parsed.getMajor() + "." + parsed.getMinor();
		} else {
			return parsed.getMajor() + "." + parsed.getMinor() + "." + parsed.getMicro();
		}
	}

	/** Returns a map from every bundle-id to its corresponding 3-part version (the qualifier is dropped). */
	public static Map<String, String> bundleToVersion(EclipseRelease release) {
		return createVersionMap(release, Function.identity());
	}

	/** Returns a map from every groupId:artifactId to its corresponding version in maven central (the qualifier is dropped). */
	public static Map<String, String> groupIdArtifactIdToVersion(EclipseRelease release) {
		return createVersionMap(release, MavenCentralMapping::groupIdArtifactId);
	}

	private static final String ARTIFACTS_JAR = "artifacts.jar";

	private static Map<String, String> createVersionMap(EclipseRelease release, Function<String, String> keyExtractor) {
		//  warn if the user is asking for a too-old version of eclipse, but go ahead and try anyway just in case
		if (release.version().compareTo(FIRST_ON_CENTRAL.version()) < 0) {
			throw new IllegalArgumentException(FIRST_ON_CENTRAL.version() + " was the first eclipse release that was published on maven central, you requested " + release);
		}
		if (!release.isXYZ()) {
			throw new IllegalArgumentException("Maven central mapping requires 'x.y.z' and does not support 'x.y'.  Try " + release + ".0 instead of " + release);
		}
		File versionFolder = new File(GoomphCacheLocations.eclipseReleaseMetadata(), release.version().toString());
		FileMisc.mkdirs(versionFolder);
		File artifactsJar = new File(versionFolder, ARTIFACTS_JAR);
		if (artifactsJar.exists() && artifactsJar.length() > 0) {
			try {
				return parseFromFile(artifactsJar, keyExtractor);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Retrying download...");
				FileMisc.forceDelete(artifactsJar);
			}
		}
		return Errors.rethrow().get(() -> {
			FileMisc.download(release.updateSite() + "artifacts.jar", artifactsJar);
			return parseFromFile(artifactsJar, keyExtractor);
		});
	}

	private static Map<String, String> parseFromFile(File artifactsJar, Function<String, String> keyExtractor) throws IOException {
		Box.Nullable<Map<String, String>> value = Box.Nullable.ofNull();
		ZipMisc.read(artifactsJar, "artifacts.xml", input -> {
			value.set(Errors.rethrow().get(() -> parse(input, keyExtractor)));
		});
		return Objects.requireNonNull(value.get());
	}
}
