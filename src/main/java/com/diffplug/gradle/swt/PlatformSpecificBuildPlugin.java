/*
 * Copyright (C) 2021-2023 DiffPlug
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;

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
				systemProp -> get(settings, settings.getProviders().systemProperty(systemProp)),
				envVar -> get(settings, settings.getProviders().environmentVariable(envVar)),
				cmds -> get(settings, settings.getProviders().exec(e -> {
					e.commandLine(cmds.toArray());
				}).getStandardOutput().getAsText()));
	}

	private <T> T get(Settings settings, Provider<T> provider) {
		if (badSemver(settings.getGradle().getGradleVersion()) >= badSemver(STOP_FORUSE_AT_CONFIGURATION_TIME)) {
			return provider.get();
		} else {
			return provider.forUseAtConfigurationTime().get();
		}
	}

	static final String STOP_FORUSE_AT_CONFIGURATION_TIME = "7.4";

	private static final Pattern BAD_SEMVER = Pattern.compile("(\\d+)\\.(\\d+)");

	private static int badSemver(String input) {
		Matcher matcher = BAD_SEMVER.matcher(input);
		if (!matcher.find() || matcher.start() != 0) {
			throw new IllegalArgumentException("Version must start with " + BAD_SEMVER.pattern());
		}
		String major = matcher.group(1);
		String minor = matcher.group(2);
		return badSemver(Integer.parseInt(major), Integer.parseInt(minor));
	}

	/** Ambiguous after 2147.483647.blah-blah */
	private static int badSemver(int major, int minor) {
		return major * 1_000_000 + minor;
	}
}
