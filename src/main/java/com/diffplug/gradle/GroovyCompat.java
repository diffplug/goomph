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
package com.diffplug.gradle;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import java.util.function.Consumer;
import java.util.function.Function;

public class GroovyCompat {
	/** Creates a Groovy {@link Closure} from a Java 8 {@link Function}, uses the delegate as the input. */
	@SuppressWarnings("serial")
	public static <T> Closure<T> closureFrom(Object owner, Function<T, T> closure) {
		return new Closure<T>(owner) {
			@SuppressWarnings("unchecked")
			@Override
			public T call() {
				return closure.apply((T) getDelegate());
			}
		};
	}

	/** Creates a Groovy {@link Closure} from a Java 8 {@link Consumer}, uses the delegate as the input. */
	@SuppressWarnings("serial")
	public static <T> Closure<T> closureFrom(Object owner, Consumer<T> closure) {
		return new Closure<T>(owner) {
			@SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "This does get called from Groovy")
			@SuppressWarnings("unchecked")
			public void doCall() {
				closure.accept((T) getDelegate());
			}
		};
	}
}
