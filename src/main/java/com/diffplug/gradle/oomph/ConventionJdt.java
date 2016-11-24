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

import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Action;

/**
 * Adding the JDT convention to your project
 * adds the following features:
 * 
 * - `org.eclipse.platform.ide`
 * - `org.eclipse.jdt`
 * - `org.eclipse.ui.views.log`
 * 
 * You can set the installed JRE as follows:
 * 
 * ```gradle
 * oomphIde {
 *     jdt {
 *         installedJre {
 *             version = '1.6.0_45'
 *             installedLocation = new File('C:/jdk1.6.0_45')
 *             markDefault = true // or false
 *             executionEnvironments = ['JavaSE-1.6'] // any execution environments can be specified here.
 *         }
 *     }
 * }
 * ```
 */
public class ConventionJdt extends OomphConvention {
	ConventionJdt(OomphIdeExtension extension) {
		super(extension);
		requireIUs(IUs.IDE, IUs.JDT, IUs.ERROR_LOG);
		setPerspectiveOver(Perspectives.JAVA, Perspectives.RESOURCES);
	}

	final Set<InstalledJre> installedJres = new HashSet<>();

	/** Adds an installed JRE with the given content. */
	public void installedJre(Action<InstalledJre> action) {
		InstalledJre instance = new InstalledJre();
		action.execute(instance);
		installedJres.add(instance);
	}

	@Override
	public void close() {
		// add installed jres
		if (!installedJres.isEmpty()) {
			extension.addSetupAction(new InstalledJreAdder(installedJres));
		}
	}
}
