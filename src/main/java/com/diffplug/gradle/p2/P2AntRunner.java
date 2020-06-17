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


import com.diffplug.gradle.eclipserunner.EclipseApp;
import com.diffplug.gradle.pde.EclipseRelease;
import com.diffplug.gradle.pde.PdeInstallation;
import groovy.util.Node;
import org.gradle.api.Action;
import org.gradle.api.Project;

/**
 * Models an eclipse ant task which can be run by the P2 Bootstrapper.
 * 
 * Other useful tasks are [here](https://help.eclipse.org/neon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_repositorytasks.htm).
 * 
 * If the task you require isn't contained in the p2 bootstrapper,
 * you can also run the task using a full PDE installation using
 * {@link #runUsingPDE(Project)} or {@link #runUsingPDE(EclipseRelease)}.
 */
public class P2AntRunner extends EclipseApp.AntRunner {
	/** Creates an ant task of the given type, configued by the `setup` action. */
	public static P2AntRunner create(String taskType, Action<Node> setup) {
		Node rootTask = new Node(null, taskType);
		setup.execute(rootTask);
		P2AntRunner antTask = new P2AntRunner();
		antTask.setTask(rootTask);
		return antTask;
	}

	protected P2AntRunner() {}

	/** Runs this application, downloading a small bootstrapper if necessary. */
	public void runUsingBootstrapper() throws Exception {
		runUsing(P2BootstrapInstallation.latest().outsideJvmRunner());
	}

	/** Runs this application, downloading a small bootstrapper if necessary. */
	public void runUsingBootstrapper(Project project) throws Exception {
		runUsing(P2BootstrapInstallation.latest().outsideJvmRunner(project));
	}

	/** Runs this application, using PDE as specified by {@link PdeInstallation#fromProject(Project)}. */
	public void runUsingPDE(Project project) throws Exception {
		runUsing(PdeInstallation.fromProject(project));
	}

	/** Runs this application, using PDE as specified by {@link PdeInstallation#from(EclipseRelease)}. */
	public void runUsingPDE(EclipseRelease release) throws Exception {
		runUsing(PdeInstallation.from(release));
	}
}
