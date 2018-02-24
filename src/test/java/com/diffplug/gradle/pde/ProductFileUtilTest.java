package com.diffplug.gradle.pde;

import org.gradle.api.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Properties;

public class ProductFileUtilTest {

	@Test
	public void testIncludeLauncherTrue() {

		String launcherTrue = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" includeLaunchers=\"true\">";

		String property = ProductFileUtil.extractProperties(new String[]{launcherTrue}).getProperty("includeLaunchers");

		Assert.assertEquals("true", property);
	}

	@Test
	public void testIncludeLauncherFalse() {

		String launcherFalse = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" includeLaunchers=\"false\">";

		String property = ProductFileUtil.extractProperties(new String[]{launcherFalse}).getProperty("includeLaunchers");

		Assert.assertEquals("false", property);
	}

	@Test
	public void testIncludeLauncherEmpty() {

		String launcherEmpty = "<product uid=\"com.diffplug.gradle.pde.product\" id=\"com.diffplug.gradle.pde.productId\" application=\"org.eclipse.ui.ide.workbench\" version=\"1.0.0\" useFeatures=\"false\" >";

		String property = ProductFileUtil.extractProperties(new String[]{launcherEmpty}).getProperty("includeLaunchers");

		Assert.assertNull(property);
	}
}
