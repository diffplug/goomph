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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.diffplug.gradle.pde.EclipseRelease;

public class MavenCentralMappingTest {
	@Test
	public void testParsing() throws IOException, ParserConfigurationException, SAXException {
		try (InputStream input = MavenCentralMappingTest.class.getResourceAsStream("/artifacts-4.6.3.xml")) {
			assert463(MavenCentralMapping.parse(input));
		}
	}

	@Test
	public void testComplete() throws IOException {
		assert463(MavenCentralMapping.bundleToVersion(EclipseRelease.official("4.6.3")));
	}

	private void assert463(Map<String, String> bundleToVersion) {
		Assertions.assertThat(bundleToVersion)
				.containsEntry("org.eclipse.debug.core", "3.10.100")
				.containsEntry("org.eclipse.equinox.p2.metadata", "2.3.100")
				.hasSize(895);
	}
}
