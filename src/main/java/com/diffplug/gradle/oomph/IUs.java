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

import com.diffplug.gradle.p2.P2Model;

/**
 * A listing of common IUs.
 * 
 * Javadoc includes the repositories which contain the IU.
 */
public class IUs {
	private IUs() {}

	private static final String FEATURE_GROUP = ".feature.group";

	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String IDE = "org.eclipse.platform.ide";
	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String JDT = "org.eclipse.jdt" + FEATURE_GROUP;
	/** Required IU for every IDE, contained within {@link P2Model#addRepoEclipse(String)}. */
	public static final String PDE = "org.eclipse.pde" + FEATURE_GROUP;
	/** The error log view.  Included in the PDE, but helpful in lots of other places too. */
	public static final String ERROR_LOG = "org.eclipse.ui.views.log";
}
