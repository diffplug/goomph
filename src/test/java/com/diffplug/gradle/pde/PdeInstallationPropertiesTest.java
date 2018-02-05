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
package com.diffplug.gradle.pde;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

public class PdeInstallationPropertiesTest {

	@Test
	public void testUpdateSiteProperty() {

		String expectedSite = "newUpdateSite";
		String expectedId = "pdeId";

		Project project = ProjectBuilder.builder().build();
		ExtraPropertiesExtension props = project.getExtensions().getExtraProperties();

		props.set("GOOMPH_PDE_VER", "1.0.0");
		props.set("GOOMPH_PDE_UPDATE_SITE", expectedSite);
		props.set("GOOMPH_PDE_ID", expectedId);

		PdeInstallation install = PdeInstallation.fromProject(project);
		Assert.assertEquals(expectedSite, install.release.updateSite);
		Assert.assertEquals(expectedId, install.release.id);
	}

	@Test
	public void testUpdateSitePropertyDownwardsCompatibilityDueTypo() {

		String expectedSite = "oldUdpateSite";
		String expectedId = "pdeId";

		Project project = ProjectBuilder.builder().build();
		ExtraPropertiesExtension props = project.getExtensions().getExtraProperties();

		props.set("GOOMPH_PDE_VER", "1.0.0");
		props.set("GOOMPH_PDE_UDPATE_SITE", expectedSite);
		props.set("GOOMPH_PDE_ID", expectedId);

		PdeInstallation install = PdeInstallation.fromProject(project);
		Assert.assertEquals(expectedSite, install.release.updateSite);
		Assert.assertEquals(expectedId, install.release.id);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullUpdateSiteProperty() {

		Project project = ProjectBuilder.builder().build();
		ExtraPropertiesExtension props = project.getExtensions().getExtraProperties();

		props.set("GOOMPH_PDE_UPDATE_SITE", null);

		PdeInstallation install = PdeInstallation.fromProject(project);
	}
}
