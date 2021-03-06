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
package com.diffplug.gradle.oomph.thirdparty;


import com.diffplug.gradle.oomph.OomphConvention;
import com.diffplug.gradle.oomph.OomphIdeExtension;
import org.gradle.api.Action;

/**
 * This is a place for third-party modules to add their own
 * configuration logic.  It's a bit wild-west.
 */
public class ConventionThirdParty extends OomphConvention {
	public ConventionThirdParty(OomphIdeExtension extension) {
		super(extension);
	}

	/** Adds an in-eclipse terminal, @see ConventionTmTerminal. */
	public void tmTerminal(Action<ConventionTmTerminal> action) {
		OomphConvention.configure(ConventionTmTerminal::new, extension, action);
	}

	/** Adds syntax highlighting for gradle scripts, @see ConventionMinimalistGradleEditor. */
	public void minimalistGradleEditor(Action<ConventionMinimalistGradleEditor> action) {
		OomphConvention.configure(ConventionMinimalistGradleEditor::new, extension, action);
	}

	/** Adds gradle integration, @see ConventionBuildship. */
	public void buildship(Action<ConventionBuildship> action) {
		OomphConvention.configure(ConventionBuildship::new, extension, action);
	}
}
