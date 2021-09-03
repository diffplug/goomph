/*
 * Copyright (C) 2021 DiffPlug
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
package com.diffplug.gradle.swt;


import com.diffplug.common.swt.os.OS;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/**
 * In order to detect the underlying operating system and architecture, it is necessary to
 * to read various system properties and environment variables, which breaks the Gradle configuration cache.
 * But, if you apply `com.diffplug.configuration-cache-for-platform-specific-build` in your `settings.gradle`,
 * then you can call {@link OS#getRunning()} and {@link OS#getNative()} and behind the scenes it will use
 * <a href="https://docs.gradle.org/current/userguide/configuration_cache.html#config_cache:requirements:undeclared_sys_prop_read">
 * the appropriate APIs</a> which don't break the configuration cache.
 */
public class PlatformSpecificBuildPlugin implements Plugin<Settings> {
	@Override
	public void apply(Settings settings) {
		OS.detectPlatform(
				systemProp -> settings.getProviders().systemProperty(systemProp).forUseAtConfigurationTime().get(),
				envVar -> settings.getProviders().environmentVariable(envVar).forUseAtConfigurationTime().get());
	}
}
