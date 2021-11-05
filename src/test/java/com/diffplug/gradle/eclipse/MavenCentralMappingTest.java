/*
 * Copyright (C) 2015-2021 DiffPlug
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


import com.diffplug.gradle.pde.EclipseRelease;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.xml.sax.SAXException;

public class MavenCentralMappingTest {
	@Test
	public void testParsing463() throws IOException, ParserConfigurationException, SAXException {
		try (InputStream input = MavenCentralMappingTest.class.getResourceAsStream("/artifacts-4.6.3.xml")) {
			assert463(MavenCentralMapping.parse(input, Function.identity()));
		}
	}

	@Test
	public void testCompleteBundleToVersion() throws IOException {
		assert463(MavenCentralMapping.bundleToVersion(EclipseRelease.official("4.6.3")));
	}

	private void assert463(Map<String, String> bundleToVersion) {
		Assertions.assertThat(bundleToVersion)
				.containsEntry("org.eclipse.debug.core", "3.10.100")
				.containsEntry("org.eclipse.equinox.p2.metadata", "2.3.100")
				.containsEntry("org.eclipse.help", "3.7.0")
				.hasSize(743);
	}

	@Test
	public void testParsing463GroupIdArtifactId() throws IOException, ParserConfigurationException, SAXException {
		try (InputStream input = MavenCentralMappingTest.class.getResourceAsStream("/artifacts-4.6.3.xml")) {
			assert463GroupIdArtifactId(MavenCentralMapping.parse(input, MavenCentralMapping::groupIdArtifactId));
		}
	}

	@Test
	public void testCompleteGroupIdArtifactIdToVersion() throws IOException {
		assert463GroupIdArtifactId(MavenCentralMapping.groupIdArtifactIdToVersion(EclipseRelease.official("4.6.3")));
	}

	private void assert463GroupIdArtifactId(Map<String, String> groupIdArtifactIdToVersion) {
		Assertions.assertThat(groupIdArtifactIdToVersion)
				.containsEntry("org.eclipse.platform:org.eclipse.debug.core", "3.10.100")
				.containsEntry("org.eclipse.platform:org.eclipse.equinox.p2.metadata", "2.3.100")
				.containsEntry("org.eclipse.platform:org.eclipse.help", "3.7.0")
				.containsEntry("org.eclipse.ecf:org.eclipse.ecf", "3.8.0")
				.containsEntry("org.eclipse.jdt:org.eclipse.jdt.core", "3.12.3")
				.containsEntry("org.eclipse.jdt:ecj", "3.12.3")
				.containsEntry("com.ibm.icu:icu4j", "56.1")
				.hasSize(743);
	}

	@Test
	public void testParsing4140() throws IOException, ParserConfigurationException, SAXException {
		try (InputStream input = MavenCentralMappingTest.class.getResourceAsStream("/artifacts-4.14.0.xml")) {
			assert4140(MavenCentralMapping.parse(input, MavenCentralMapping::groupIdArtifactId));
		}
	}

	private void assert4140(Map<String, String> groupIdArtifactIdToVersion) {
		Assertions.assertThat(groupIdArtifactIdToVersion)
				.containsEntry("org.eclipse.platform:org.eclipse.debug.core", "3.14.100")
				.containsEntry("org.eclipse.platform:org.eclipse.equinox.p2.metadata", "2.4.600")
				.containsEntry("org.eclipse.platform:org.eclipse.help", "3.8.600")
				.containsEntry("org.eclipse.ecf:org.eclipse.ecf", "3.9.4")
				.containsEntry("org.eclipse.jdt:org.eclipse.jdt.core", "3.20.0")
				.containsEntry("org.eclipse.jdt:ecj", "3.20.0")
				.containsEntry("com.ibm.icu:icu4j", "64.2")
				.hasSize(785);
	}

	@Test
	public void testMissingBugfixVersion() throws IOException {
		Assertions.assertThatThrownBy(() -> {
			MavenCentralMapping.bundleToVersion(EclipseRelease.official("4.14"));
		}).hasMessage("Maven central mapping requires 'x.y.z' and does not support 'x.y'.  Try 4.14.0 instead of 4.14");
	}

	@Test
	public void testCalculateMavenCentralVersion() {
		Assertions.assertThat(MavenCentralMapping.calculateMavenCentralVersion("com.ibm.icu", "7.1.0.v20990507-1337")).isEqualTo("7.1");
		Assertions.assertThat(MavenCentralMapping.calculateMavenCentralVersion("com.ibm.icu", "7.1.2.v20990806-1429")).isEqualTo("7.1.2");
		Assertions.assertThat(MavenCentralMapping.calculateMavenCentralVersion("org.eclipse.jdt.core", "3.10.000.v20991402-2332")).isEqualTo("3.10.0");
		Assertions.assertThat(MavenCentralMapping.calculateMavenCentralVersion("org.eclipse.jdt.core", "3.10.100.v20991507-1246")).isEqualTo("3.10.100");
	}
}
