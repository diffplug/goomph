/*
 * Copyright (C) 2015-2019 DiffPlug
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
package com.diffplug.gradle.pde;


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.io.Resources;
import com.diffplug.common.swt.os.SwtPlatform;
import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Takes `org.eclipse.pde.build_{version}/templates/headless-build/build.properties`
 * and wraps it in an API.
 */
class PdeBuildProperties {
	/** Returns the content of template.build.properties as a String. */
	static String rawFile() {
		return Errors.rethrow().get(() -> {
			URL url = PdeBuildProperties.class.getResource("template.build.properties");
			byte[] content = Resources.toByteArray(url);
			return new String(content, StandardCharsets.UTF_8);
		});
	}

	private String content;

	public PdeBuildProperties() {
		content = rawFile();
	}

	String getContent() {
		return content;
	}

	/** Sets the base properties to match the running platform. */
	void setBasePlatform(SwtPlatform host) {
		setProp("baseos", host.getOs());
		setProp("basews", host.getWs());
		setProp("basearch", host.getArch());
	}

	/** Sets the build directory. */
	void setBuildDirectory(File buildDir) {
		setProp("buildDirectory", buildDir.getAbsolutePath());
	}

	/** Sets the platforms which will be built. */
	void setConfigs(Collection<SwtPlatform> platforms) {
		String value = platforms.stream()
				.map(plat -> plat.getOs() + "," + plat.getWs() + "," + plat.getArch())
				.collect(Collectors.joining(" & \n"));
		setProp("configs", value);
	}

	/** Returns the system path separator. */
	private static String getSep() {
		return System.getProperty("path.separator");
	}

	/** Adds the given Files to the plugin path, (the eclipse delta pack is automatically added. */
	void setPluginPaths(List<File> pluginPaths) {
		setProp("pluginPath", Joiner.on(getSep()).join(pluginPaths));
	}

	/** Sets the JRE and java targets which we're compiling against to Java 8. */
	void setJDK(JdkConfig jdk) {
		String jdkLibs = jdk.getJdkLibs().stream()
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(getSep()));
		setTag("JRE", jdk.name + "=\"" + jdkLibs + "\"");

		setProp("javacSource", jdk.source);
		setProp("javacTarget", jdk.target);
	}

	/** Sets the given tag to the given value. */
	private void setTag(String tag, String val) {
		replace(tag, val);
	}

	/** Sets the given tag to the given value if possible, else adds the given property. */
	void setProp(String key, String value) {
		if (content.contains(tag_(key))) {
			replace(key, key + "=" + value);
		} else {
			if (!content.endsWith("\n")) {
				content += "\n";
			}
			content += key + "=" + val_(value);
			content += "\n";
		}
	}

	/** Replaces the given tag with the given value. */
	private void replace(String tag, String value) {
		String modified = content.replace(tag_(tag), val_(value));
		Preconditions.checkState(!modified.equals(content), "No effect: %s to %s", tag, value);
		this.content = modified;
	}

	/** Tag-ifies a string. */
	private static String tag_(String key) {
		return "#{" + key + "}";
	}

	/** Adds a \ to newlines, which is the java properties way to continue on a new line. */
	private static String val_(String val) {
		return val.replace("\\", "/").replace("\n", "\\\n");
	}
}
