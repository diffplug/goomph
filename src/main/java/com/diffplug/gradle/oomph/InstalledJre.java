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
package com.diffplug.gradle.oomph;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/** Simple representation of a JRE */
public class InstalledJre implements Serializable {
	private static final long serialVersionUID = 8530657374964977698L;

	private String version;
	private File installedLocation;
	private boolean markDefault;
	private List<String> executionEnvironments;

	public String getVersion() {
		return version;
	}

	public void setVersion(String name) {
		this.version = name;
	}

	public File getInstalledLocation() {
		return installedLocation;
	}

	public void setInstalledLocation(File location) {
		this.installedLocation = location;
	}

	public boolean isMarkDefault() {
		return markDefault;
	}

	public void setMarkDefault(boolean markDefault) {
		this.markDefault = markDefault;
	}

	public List<String> getExecutionEnvironments() {
		return executionEnvironments;
	}

	public void setExecutionEnvironments(List<String> executionEnvironments) {
		this.executionEnvironments = executionEnvironments;
	}
}
