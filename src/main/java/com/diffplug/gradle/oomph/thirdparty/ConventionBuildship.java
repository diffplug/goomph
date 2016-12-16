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
package com.diffplug.gradle.oomph.thirdparty;

import com.diffplug.gradle.oomph.IUs;
import com.diffplug.gradle.oomph.OomphIdeExtension;

/**
 * Adds [Buildship](http://projects.eclipse.org/projects/tools.buildship).
 */
public class ConventionBuildship extends WithRepoConvention {
	public static final String REPO = "http://download.eclipse.org/buildship/updates/e46/releases/1.0/1.0.21.v20161010-1640/";
	public static final String FEATURE = "org.eclipse.buildship";

	ConventionBuildship(OomphIdeExtension extension) {
		super(extension, REPO);
		requireIUs(IUs.featureGroup(FEATURE));
	}
}
