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
import com.diffplug.gradle.oomph.OomphConvention;
import com.diffplug.gradle.oomph.OomphIdeExtension;

/**
 * Adds the [minimalist gradle editor](https://github.com/Nodeclipse/nodeclipse-1/tree/master/org.nodeclipse.enide.editors.gradle).
 * 
 * - repo: `http://www.nodeclipse.org/updates/gradle-ide-pack/`
 * - feature: `org.nodeclipse.enide.editors.gradle.feature`
 */
public class ConventionMinimalistGradleEditor extends OomphConvention {
	private static final String REPO = "http://www.nodeclipse.org/updates/gradle-ide-pack/";
	private static final String IU = IUs.featureGroup("org.nodeclipse.enide.editors.gradle.feature");

	ConventionMinimalistGradleEditor(OomphIdeExtension extension) {
		super(extension);
		extension.getP2().addRepo(REPO);
		requireIUs(IU);
	}
}
