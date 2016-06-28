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
import java.util.ArrayList;
import java.util.Collection;

import com.diffplug.gradle.osgi.OsgiExecable;

/** Sets the default perspective. */
class TargetPlatformSetter extends OsgiExecable.ReflectionHost {
	private static final long serialVersionUID = 3285583309500818867L;

	String name;
	ArrayList<File> targetPlatforms;

	public TargetPlatformSetter(String name, Collection<File> targetPlatforms) {
		super("com.diffplug.gradle.oomph.TargetPlatformSetterInternal");
		this.name = name;
		this.targetPlatforms = new ArrayList<>(targetPlatforms);
	}
}
