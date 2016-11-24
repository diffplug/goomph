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

import java.util.Arrays;

/**
 * Base class for implementing a DSL
 * around a specific part of the IDE.
 */
public class OomphConvention implements AutoCloseable {
	protected final OomphIdeExtension extension;

	OomphConvention(OomphIdeExtension extension) {
		this.extension = extension;
	}

	/** Ensures the p2 model contains the given IUs. */
	protected void requireIUs(String... ius) {
		for (String iu : ius) {
			extension.getP2().addIU(iu);
		}
	}

	/**
	 * Sets the perspective to the first value, if the existing value is one of the other values.
	 * 
	 * This allows a consistent mechanism for PDE to trump JDT.
	 */
	protected void setPerspectiveOver(String toSet, String... toTrump) {
		if (Arrays.asList(toTrump).contains(extension.perspective)) {
			extension.perspective(toSet);
		}
	}

	/**
	 * This is called when the convention block ends.
	 * 
	 * Usually it can just be empty, but if you've been accumulating
	 * values, this is your chance to smush them down into
	 * a setup action (see {@link ConventionJdt}. 
	 */
	@Override
	public void close() {}
}
