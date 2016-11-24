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
import java.util.Objects;

import org.gradle.api.Action;

import com.diffplug.gradle.OrderingConstraints;

/**
 * Adding the PDE convention to your project
 * adds the following features:
 * 
 * - `org.eclipse.platform.ide`
 * - `org.eclipse.jdt`
 * - `org.eclipse.pde`
 * 
 * You can set the targetplatform as follows:
 * 
 * ```gradle
 * oomphIde {
 *     pde {
 *         targetplatform {
 *             it.installation '../target.maven/build'
 *             it.installation '../target.p2/build/p2asmaven/p2runnable/eclipse-deps'
 *         }
 *     }
 * }
 * ```
 */
public class ConventionPde extends OomphConvention {
	ConventionPde(OomphIdeExtension extension) {
		super(extension);
		requireIUs(IUs.IDE, IUs.JDT, IUs.PDE);
		setPerspectiveOver(Perspectives.PDE, Perspectives.JAVA, Perspectives.RESOURCES);
	}

	/** Creates a targetplatform with the given content. */
	public void targetplatform(Action<TargetPlatform> targetplatform) {
		targetplatform("targetplatform", targetplatform);
	}

	/** Creates a targetplatform with the given name and content. */
	public void targetplatform(String name, Action<TargetPlatform> targetplatform) {
		extension.addSetupActionLazy(actions -> {
			TargetPlatform instance = new TargetPlatform();
			targetplatform.execute(instance);
			actions.add(new TargetPlatformSetter(name, instance.installations));
		});
	}

	/** Api for defining the target platform. */
	public class TargetPlatform {
		ArrayList<File> installations = new ArrayList<>();

		/** Adds an installation. */
		public void installation(Object installation) {
			installations.add(extension.project.file(installation));
		}
	}

	/** Sets the target platform. */
	public static class TargetPlatformSetter extends SetupAction {
		private static final long serialVersionUID = 3285583309500818867L;

		String name;
		ArrayList<File> installations;

		public TargetPlatformSetter(String name, ArrayList<File> installations) {
			super("com.diffplug.gradle.oomph.ConventionPdeTargetPlatformSetter");
			this.name = Objects.requireNonNull(name);
			this.installations = Objects.requireNonNull(installations);
		}

		@Override
		protected void populateOrdering(OrderingConstraints<Class<? extends SetupAction>> ordering) {
			// we must set a targetplatform before importing projects
			ordering.before(ProjectImporter.class);
		}

		@Override
		public String getDescription() {
			return "create targetplatform";
		}
	}
}
