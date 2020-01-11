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
package com.diffplug.gradle;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.gradle.api.Action;

/**
 * Represents a mutable element, probably
 * a collection, which can be mutated directly,
 * or lazily using the {@link Action} mechanism.
 * 
 * If the actions are applied to the root mutable element,
 * then it can be evaluated only once.  Lazyable allows
 * the actions to be evaluated many times, without modifying
 * the underlying root.
 */
public class Lazyable<C> {
	final Function<? super C, ? extends C> copier;
	final C root;
	final List<Action<? super C>> actions = new ArrayList<Action<? super C>>();

	/** 
	 * @param root		The original element.
	 * @param copier	Copies the type, so the actions can be applied to it.
	 */
	public Lazyable(C root, Function<? super C, ? extends C> copier) {
		this.root = Objects.requireNonNull(root);
		this.copier = Objects.requireNonNull(copier);
	}

	/** Returns the root object. */
	public C getRoot() {
		return root;
	}

	/** Adds an action which will act on a copy of the root collection. */
	public void addLazyAction(Action<? super C> action) {
		actions.add(action);
	}

	/** Returns the final result. */
	public C getResult() {
		C copied = copier.apply(root);
		actions.forEach(action -> action.execute(copied));
		return copied;
	}

	/** Returns a Lazyable wrapper around a list. */
	@SuppressWarnings("unchecked")
	public static <T> Lazyable<List<T>> ofList() {
		return (Lazyable<List<T>>) (Object) ofArrayList();
	}

	/** Returns a Lazyable wrapper around an ArrayList. */
	public static <T> Lazyable<ArrayList<T>> ofArrayList() {
		return new Lazyable<>(new ArrayList<>(), toCopy -> new ArrayList<>(toCopy));
	}
}
