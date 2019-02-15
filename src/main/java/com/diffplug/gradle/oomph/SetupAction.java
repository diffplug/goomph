/*
 * Copyright 2019 DiffPlug
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
package com.diffplug.gradle.oomph;

import java.util.List;

import com.diffplug.gradle.OrderingConstraints;
import com.diffplug.gradle.osgi.OsgiExecable;

/**
 * Setup action which takes place within the Eclipse installation.
 */
public abstract class SetupAction extends OsgiExecable.ReflectionHost {
	private static final long serialVersionUID = 127527019605826401L;

	protected SetupAction(String client) {
		super(client);
	}

	/** `Executing <getDescription>... done.` */
	public abstract String getDescription();

	/** The ordering constraints on this setup action. */
	public final OrderingConstraints<Class<? extends SetupAction>> getOrdering() {
		OrderingConstraints<Class<? extends SetupAction>> ordering = new OrderingConstraints<Class<? extends SetupAction>>();
		populateOrdering(ordering);
		return ordering;
	}

	/** Populates the ordering constraints. */
	protected void populateOrdering(OrderingConstraints<Class<? extends SetupAction>> ordering) {}

	@Override
	public final String toString() {
		return getDescription();
	}

	/** Orders the given setup actions according to their ordering constrains, if any. */
	public static List<SetupAction> order(List<SetupAction> input) {
		return OrderingConstraints.satisfy(input, SetupAction::getClass, SetupAction::getOrdering);
	}

	public static abstract class Internal<Host extends SetupAction> extends OsgiExecable.ReflectionClient<Host> {
		protected Internal(Host host) {
			super(host);
		}

		protected abstract void runWithinEclipse() throws Throwable;

		@Override
		public final void run() {
			try {
				runWithinEclipse();
			} catch (Throwable error) {
				EclipseMisc.logException(error);
			}
			EclipseMisc.waitForJobsToFinish();
		}
	}
}
