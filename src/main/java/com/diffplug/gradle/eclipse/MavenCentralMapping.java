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
package com.diffplug.gradle.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.diffplug.common.base.Box;
import com.diffplug.common.base.Errors;
import com.diffplug.common.io.Files;
import com.diffplug.common.io.Resources;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.ZipMisc;
import com.diffplug.gradle.pde.EclipseRelease;

/** Maps eclipse jars to their mavenCentral artifact ids and versions based on their official release. */
public class MavenCentralMapping {
	private static final EclipseRelease FIRST_ON_CENTRAL = EclipseRelease.official("4.6.2");

	private static final String PLATFORM = "org.eclipse.platform";
	private static final String JDT = "org.eclipse.jdt";
	private static final String PDE = "org.eclipse.pde";

	/** Returns the MavenCentral groupId:artifactId appropriate for the given bundleId. */
	public static String groupIdArtifactId(String bundleId) {
		if (bundleId.startsWith(JDT)) {
			return JDT + ":" + bundleId;
		} else if (bundleId.startsWith(PDE)) {
			return PDE + ":" + bundleId;
		} else {
			return PLATFORM + ":" + bundleId;
		}
	}

	/** Creates a map from bundle-id to its corresponding 3-part version. */
	static Map<String, String> parse(InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
		Map<String, String> map = new HashMap<>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream);
		Node artifacts = doc.getDocumentElement().getElementsByTagName("artifacts").item(0);
		for (int i = 0; i < artifacts.getChildNodes().getLength(); ++i) {
			Node artifact = artifacts.getChildNodes().item(i);
			if ("artifact".equals(artifact.getNodeName())) {
				String id = artifact.getAttributes().getNamedItem("id").getNodeValue();
				String version = artifact.getAttributes().getNamedItem("version").getNodeValue();
				Version parsed = Version.parseVersion(version);
				map.put(id, parsed.getMajor() + "." + parsed.getMinor() + "." + parsed.getMicro());
			}
		}
		return map;
	}

	private static final String ARTIFACTS_JAR = "artifacts.jar";

	/** Returns a map from every bundle-id to its corresponding 3-part version (the qualifier is dropped). */
	public static Map<String, String> bundleToVersion(EclipseRelease release) {
		//  warn if the user is asking for a too-old version of eclipse, but go ahead and try anyway just in case
		if (release.version().compareTo(FIRST_ON_CENTRAL.version()) < 0) {
			System.err.println(FIRST_ON_CENTRAL.version() + " was the first eclipse release that was published on MavenCentral.");
		}
		File versionFolder = new File(GoomphCacheLocations.eclipseReleaseMetadata(), release.version().toString());
		FileMisc.mkdirs(versionFolder);
		File artifactsJar = new File(versionFolder, ARTIFACTS_JAR);
		if (artifactsJar.exists() && artifactsJar.length() > 0) {
			try {
				return parseFromFile(artifactsJar);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Retrying download...");
				FileMisc.forceDelete(artifactsJar);
			}
		}
		return Errors.rethrow().get(() -> {
			byte[] content = Resources.toByteArray(new URL(release.updateSite() + "artifacts.jar"));
			Files.write(content, artifactsJar);
			return parseFromFile(artifactsJar);
		});
	}

	private static Map<String, String> parseFromFile(File artifactsJar) throws IOException {
		Box.Nullable<Map<String, String>> value = Box.Nullable.ofNull();
		ZipMisc.read(artifactsJar, "artifacts.xml", input -> {
			value.set(Errors.rethrow().get(() -> parse(input)));
		});
		return Objects.requireNonNull(value.get());
	}
}
