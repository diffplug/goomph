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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.diffplug.gradle.OrderingConstraints;

/** Used for adding JRE/JDK installations to an Eclipse install. */
public class InstalledJreAdder extends SetupAction {
	private static final long serialVersionUID = -7101059764345094433L;

	final List<InstalledJre> installedJres;

	protected InstalledJreAdder(Collection<InstalledJre> jresToAdd) {
		super("com.diffplug.gradle.oomph.InstalledJreAdderInternal");
		installedJres = new ArrayList<>(jresToAdd);
	}

	@Override
	protected void populateOrdering(OrderingConstraints<Class<? extends SetupAction>> ordering) {
		// we must add installed jre(s) before importing projects
		ordering.before(ProjectImporter.class);
	}

	/**
	 * @see com.diffplug.gradle.oomph.SetupAction#getDescription()
	 */
	@Override
	public String getDescription() {
		return "adding installed JRE's";
	}
}
