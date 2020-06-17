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
package com.diffplug.gradle.p2;


import java.io.File;
import java.util.Objects;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.internal.Actions;

/** DSL for a single maven group. */
public class AsMavenGroup implements P2Declarative {
	final String group;
	final P2Model model = new P2Model();
	boolean repo2runnable = false;
	Action<P2AntRunner> antModifier = Actions.doNothing();

	public AsMavenGroup(String group) {
		this.group = Objects.requireNonNull(group);
	}

	@Override
	public P2Model getP2() {
		return model;
	}

	/** Marks that this repository will be expanded into its runnable form using {@link Repo2Runnable}. */
	public void repo2runnable() {
		repo2runnable = true;
	}

	/** Allows for fine-grained manipulation of the mirroring operation. */
	public void p2ant(Action<P2AntRunner> antModifier) {
		this.antModifier = Objects.requireNonNull(antModifier);
	}

	/** Runs the tasks defined by this p2asmaven. */
	AsMavenGroupImpl run(Project project, File p2asmaven) throws Exception {
		AsMavenGroupImpl impl = new AsMavenGroupImpl(project, p2asmaven, this);
		impl.run();
		return impl;
	}
}
