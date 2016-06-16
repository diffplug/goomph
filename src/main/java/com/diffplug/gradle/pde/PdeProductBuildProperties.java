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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Project;

import com.diffplug.common.base.Joiner;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.collect.Lists;
import com.diffplug.common.io.Resources;
import com.diffplug.common.swt.os.SwtPlatform;
import com.diffplug.gradle.JDK;

/**
 * Takes the template.build.properties and provides an API for setting
 * pieces of it. (It lives in the src/main/resources directory).
 */
class PdeProductBuildProperties {
	/** Returns the content of template.build.properties as a String. */
	static String rawFile() {
		try {
			URL url = PdeProductBuildProperties.class.getResource("/template.build.properties");
			return Resources.toString(url, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String content;

	public PdeProductBuildProperties(Project project) {
		content = rawFile();

		setJava8(project);
		setBasePlatform();
	}

	public String getContent() {
		return content;
	}

	/** Sets the build directory. */
	public void setBuildDirectory(File buildDir) {
		setProp("buildDirectory", buildDir.getAbsolutePath());
	}

	/** Sets the platforms which will be built. */
	public void setConfigs(Collection<SwtPlatform> platforms) {
		String value = platforms.stream()
				.map(plat -> plat.getOs() + "," + plat.getWs() + "," + plat.getArch())
				.collect(Collectors.joining(" & \n"));
		setProp("configs", value);
	}

	/** Returns the system path separator. */
	private static String getSep() {
		return System.getProperty("path.separator");
	}

	private List<File> pluginPath = Lists.newArrayList();

	/** Adds the given Files to the plugin path, (the eclipse delta pack is automatically added. */
	public void setPluginPath(File... dirs) {
		pluginPath.clear();
		pluginPath.addAll(Arrays.asList(dirs));
		setProp("pluginPath", Joiner.on(getSep()).join(pluginPath));
	}

	/** Returns all the paths upon which plugins will be looked up. */
	public List<File> getPluginLookupPath() {
		return pluginPath;
	}

	/** Sets the JRE and java targets which we're compiling against to Java 8. */
	private void setJava8(Project project) {
		JDK jdk = new JDK(project);
		String jdkLibs = jdk.getJdkLibs().stream()
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(getSep()));
		setTag("JRE", "JavaSE-1.8=\"" + jdkLibs + "\"");

		setProp("javacSource", "1.8");
		setProp("javacTarget", "1.8");
	}

	/** Sets the base properties to match the running platform. */
	private void setBasePlatform() {
		SwtPlatform host = SwtPlatform.getNative();
		setProp("baseos", host.getOs());
		setProp("basews", host.getWs());
		setProp("basearch", host.getArch());
	}

	/** Sets the given tag to the given value. */
	private void setTag(String tag, String val) {
		replace(tag, val);
	}

	/** Sets the given tag to the given value. */
	public void setProp(String key, String value) {
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
