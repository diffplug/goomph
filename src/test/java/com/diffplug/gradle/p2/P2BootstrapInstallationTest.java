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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Consumers;
import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.gradle.GoomphCacheLocations;
import com.diffplug.gradle.eclipse.EclipseRelease;

public class P2BootstrapInstallationTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/**
	 * Downloads p2-bootstrap from bintray (about 6MB),
	 * extracts it, runs p2-installer, then makes sure
	 * that it worked.
	 */
	@Test
	public void test() throws Exception {
		// force a download
		GoomphCacheLocations.override_p2bootstrap = folder.newFolder("p2-bootstrap");
		try {
			P2Model model = new P2Model();
			model.addRepo(EclipseRelease.official("4.5.2").updateSite());
			model.addIU("org.eclipse.core.runtime");
			File installed = folder.newFolder("installed");
			model.install(installed, "profile", Consumers.doNothing());
			File plugins = new File(installed, "plugins");
			List<String> pluginNames = Arrays.asList(plugins.listFiles()).stream()
					.map(File::getName)
					.collect(Collectors.toList());
			Assert.assertEquals(StringPrinter.buildStringFromLines(
					"org.eclipse.core.contenttype_3.5.0.v20150421-2214.jar",
					"org.eclipse.core.jobs_3.7.0.v20150330-2103.jar",
					"org.eclipse.core.runtime_3.11.1.v20150903-1804.jar",
					"org.eclipse.equinox.app_1.3.300.v20150423-1356.jar",
					"org.eclipse.equinox.common_3.7.0.v20150402-1709.jar",
					"org.eclipse.equinox.preferences_3.5.300.v20150408-1437.jar",
					"org.eclipse.equinox.registry_3.6.0.v20150318-1503.jar",
					"org.eclipse.osgi_3.10.102.v20160118-1700.jar").trim(), Joiner.on('\n').join(pluginNames));
		} finally {
			// make sure that we don't muck up the p2bootstrap location permanently
			GoomphCacheLocations.override_p2bootstrap = null;
		}
	}
}
