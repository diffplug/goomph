/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.oomph;


import com.diffplug.gradle.p2.P2Model;

/**
 * A listing of common IUs.
 * 
 * Javadoc includes the repositories which contain the IU.
 */
public class IUs {
	private IUs() {}

	public static String featureGroup(String input) {
		return input + ".feature.group";
	}

	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String IDE = "org.eclipse.platform.ide";
	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String JDT = featureGroup("org.eclipse.jdt");
	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String PDE = featureGroup("org.eclipse.pde");
	/** The error log view.  Included in the PDE, but helpful in lots of other places too. */
	public static final String ERROR_LOG = "org.eclipse.ui.views.log";
}
