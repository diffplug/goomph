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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import com.diffplug.gradle.osgi.OsgiExecable;

/** @see ProjectImporter */
class ProjectImporterInternal extends OsgiExecable.ReflectionClient<ProjectImporter> {
	ProjectImporterInternal(ProjectImporter host) {
		super(host);
	}

	@Override
	public void run() {
		// add all projects to the workspace
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		for (File projectFile : host.projects) {
			try {
				Path path = new Path(projectFile.toString());
				IProjectDescription description = workspace.loadProjectDescription(path);

				IProject project = workspace.getRoot().getProject(description.getName());
				if (project.isOpen() == false) {
					project.create(description, null);
					project.open(null);
				} else {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		// save the workspace
		try {
			boolean full = false;
			IProgressMonitor monitor = null;
			IStatus status = workspace.save(full, monitor);
			if (!status.isOK()) {
				throw new IllegalStateException(status.getMessage(), status.getException());
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}
}
