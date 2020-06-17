/*
 * Copyright (C) 2016-2019 DiffPlug
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


import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/** @see ProjectImporter */
class ProjectImporterInternal extends SetupAction.Internal<ProjectImporter> {
	ProjectImporterInternal(ProjectImporter host) {
		super(host);
	}

	@Override
	public void runWithinEclipse() throws CoreException {
		// add all projects to the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (File projectFile : host.projects) {
			Path path = new Path(projectFile.toString());
			IProjectDescription description = workspace.loadProjectDescription(path);

			IProject project = workspace.getRoot().getProject(description.getName());
			if (project.isOpen() == false) {
				project.create(description, null);
				project.open(null);
			} else {
				project.refreshLocal(IResource.DEPTH_INFINITE, null);
			}
		}
	}
}
