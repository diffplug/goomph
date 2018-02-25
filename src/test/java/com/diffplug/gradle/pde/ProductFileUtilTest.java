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

import org.junit.Assert;
import org.junit.Test;

public class ProductFileUtilTest {

	@Test
	public void testIncludeLauncherTrue() {

		String launcherTrue = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" includeLaunchers=\"true\">";

		String property = ProductFileUtil.extractProperties(new String[]{launcherTrue}).get("includeLaunchers");

		Assert.assertEquals("true", property);
	}

	@Test
	public void testIncludeLauncherFalse() {

		String launcherFalse = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" includeLaunchers=\"false\">";

		String property = ProductFileUtil.extractProperties(new String[]{launcherFalse}).get("includeLaunchers");

		Assert.assertEquals("false", property);
	}

	@Test
	public void testIncludeLauncherEmpty() {

		String launcherEmpty = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" >";

		String property = ProductFileUtil.extractProperties(new String[]{launcherEmpty}).get("includeLaunchers");

		Assert.assertNull(property);
	}
}
