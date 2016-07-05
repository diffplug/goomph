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
package com.diffplug.gradle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.StringPrinter;

/** Expresses ordering constraints on the given type. */
public class OrderingConstraints<T> {
	final List<T> requires = new ArrayList<>();
	final List<T> before = new ArrayList<>();
	final List<T> after = new ArrayList<>();

	/** This entry will now complain if the list does not also contain the given id. */
	public void require(T id) {
		requires.add(id);
	}

	/** This entry will put itself before the given id, if it is present. */
	public void beforeIfPresent(T id) {
		before.add(id);
	}

	/** This entry will put itself after the given id, if it is present. */
	public void afterIfPresent(T id) {
		after.add(id);
	}

	/** Combination of {@link #require} and {@link #beforeIfPresent}. */
	public void before(T id) {
		require(id);
		beforeIfPresent(id);
	}

	/** Combination of {@link #require} and {@link #afterIfPresent}. */
	public void after(T id) {
		require(id);
		afterIfPresent(id);
	}

	/** Returns a list which orders the given input list, meeting any constraints. */
	public static <T> List<T> satisfy(List<T> input, Function<? super T, ? extends OrderingConstraints<T>> constraintSupplier) {
		return satisfy(input, Function.identity(), constraintSupplier);
	}

	/** Returns a list which orders the given input list, meeting any constraints. */
	public static <T, C> List<T> satisfy(List<T> input, Function<? super T, ? extends C> idFunction, Function<? super T, ? extends OrderingConstraints<C>> constraintSupplier) {
		ConstrainedList<T, C> constrained = new ConstrainedList<>();
		for (T value : input) {
			constrained.add(value, idFunction.apply(value), constraintSupplier.apply(value));
		}
		return constrained.satisfyAndGet().stream()
				.map(entry -> entry.value)
				.collect(Collectors.toList());
	}

	private static class ConstrainedEntry<T, ID> {
		final T value;
		final ID id;
		final OrderingConstraints<ID> constraints;

		ConstrainedEntry(T value, ID id, OrderingConstraints<ID> constraints) {
			this.value = Objects.requireNonNull(value);
			this.id = Objects.requireNonNull(id);
			this.constraints = Objects.requireNonNull(constraints);
		}
	}

	private static class ConstrainedList<T, ID> {
		Set<ID> ids = new HashSet<>();
		List<ConstrainedEntry<T, ID>> values = new ArrayList<>();

		public void add(T value, ID id, OrderingConstraints<ID> constraints) {
			Preconditions.checkArgument(ids.add(id), "Multiple ids for %s", id);
			ConstrainedEntry<T, ID> entry = new ConstrainedEntry<>(value, id, constraints);
			values.add(entry);
		}

		public List<ConstrainedEntry<T, ID>> satisfyAndGet() {
			// satisfy requires
			for (int i = 0; i < values.size(); ++i) {
				ConstrainedEntry<T, ID> entry = values.get(i);
				for (ID required : entry.constraints.requires) {
					if (!ids.contains(required)) {
						throw new IllegalArgumentException(entry.id + " requires " + required + ", but it is not present.");
					}
				}
			}

			int numTries = 0;
			while (!isOrdered()) {
				++numTries;
				if (numTries >= values.size() * 10) {
					throw new IllegalArgumentException("Could not satisfy order constraints:\n" + orderConstraintsAsString());
				}
			}
			return Collections.unmodifiableList(values);
		}

		private int indexOf(ID id) {
			for (int i = 0; i < values.size(); ++i) {
				if (values.get(i).id.equals(id)) {
					return i;
				}
			}
			return -1;
		}

		private String orderConstraintsAsString() {
			return StringPrinter.buildString(printer -> {
				for (ConstrainedEntry<T, ID> entry : values) {
					entry.constraints.before.forEach(before -> printer.println(entry.id + " must be before " + before));
					entry.constraints.after.forEach(after -> printer.println(entry.id + " must be after " + after));
				}
			});
		}

		private boolean isOrdered() {
			for (int i = 0; i < values.size(); ++i) {
				ConstrainedEntry<T, ID> entry = values.get(i);
				// satisfy before
				for (ID before : entry.constraints.before) {
					int beforeIdx = indexOf(before);
					if (beforeIdx == -1 || i < beforeIdx) {
						// if it's not present, or if it's already before, then we're all good
						continue;
					} else {
						values.remove(i);
						values.add(beforeIdx, entry);
						return false;
					}
				}
				// satisfy after 
				for (ID after : entry.constraints.after) {
					int afterIdx = indexOf(after);
					if (afterIdx == -1 || i > afterIdx) {
						// if it's not present, or if it's already before, then we're all good
						continue;
					} else {
						values.remove(i);
						values.add(afterIdx, entry);
						return false;
					}
				}
			}
			return true;
		}
	}

}
