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
package com.diffplug.gradle.p2;

import org.gradle.api.Project;

import groovy.lang.Closure;

import com.diffplug.gradle.GroovyCompat;

/** DSL for {@link AsMavenPlugin}. */
public class AsMavenExtension {
	public static final String NAME = "p2AsMaven";

	final AsMaven mavenify;

	public AsMavenExtension(Project project) {
		this.mavenify = new AsMaven(project);
	}

	/** Sets the maven group which the artifacts will be installed into. */
	public void mavenGroup(String mavenGroup) {
		mavenify.mavenGroup(mavenGroup);
	}

	/** The location of the repository.  Defaults to `build/goomph-p2asmaven`. */
	public void destination(Object mavenGroup) {
		mavenify.destination(mavenGroup);
	}

	/** P2 model (update site and IUs). */
	public P2Model getP2() {
		return mavenify.p2();
	}

	/** Modifies args passed to p2 director. */
	public void p2args(Closure<P2Model.DirectorArgsBuilder> argsBuilder) {
		mavenify.modifyP2Args(GroovyCompat.consumerFrom(argsBuilder));
	}
}
