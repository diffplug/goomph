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
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.Task;

import com.diffplug.common.base.Errors;
import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.GoomphCacheLocations;

/**
 * Maintains a registry of goomph workspaces.
 * 
 * So here's the problem.  Let say you have a multiproject build, such as this:
 * 
 * ```
 * libA/
 *     .project
 * libB/
 *     .project
 * ide/
 *    build/oomph-ide/
 *        eclipse.exe
 *        workspace/
 * ```
 * 
 * Everything works great!  But in a single project build:
 * 
 * ```
 * lib/
 *     .project
 *     build/oomph-ide/
 *         eclipse.exe
 *         workspace/
 * ```
 * 
 * It breaks.  Why?  Because the `workspace` is a subdirectory of the `.project` folder.  And eclipse does not support that.  GAH!
 * 
 * So, to fix that, we need to maintain the workspaces in a central registry.  This class maintains
 * that registry, and cleans out old workspaces when the IDE they were created for gets deleted.
 * 
 * The registry lives in {@link GoomphCacheLocations#workspaces()}.  It names the workspace folders as such:
 * 
 * ```
 *     <gradle root project's name>-<hashcode of ide directory absolute path>/
 *     <gradle root project's name>-<hashcode of ide directory absolute path>-owner   [file containing absolute path of ide folder]
 * ```
 */
public class WorkspaceRegistry {
	public static WorkspaceRegistry instance() throws IOException {
		return new WorkspaceRegistry(GoomphCacheLocations.workspaces());
	}

	final File root;
	/** Map from the ide directory to a workspace directory. */
	final Map<File, File> ownerToWorkspace = new HashMap<>();

	static final String OWNER_PATH = "-owner";

	WorkspaceRegistry(File root) throws IOException {
		this.root = Objects.requireNonNull(root);
		FileMisc.mkdirs(root);
		for (File workspace : FileMisc.list(root)) {
			if (workspace.isDirectory()) {
				Optional<String> ownerPath = FileMisc.readToken(root, workspace.getName() + OWNER_PATH);
				if (!ownerPath.isPresent()) {
					// if there's no token, delete it
					deleteWorkspace(workspace, "missing token " + OWNER_PATH + ".");
				} else {
					ownerToWorkspace.put(new File(ownerPath.get()), workspace);
				}
			}
		}
	}

	/** Returns the workspace directory appropriate for the given project and ide folder. */
	public File workspaceDir(Project project, File ideDir) {
		return workspaceDir(project.getRootProject().getName(), ideDir);
	}

	/** Returns the workspace directory appropriate for the  */
	public File workspaceDir(Project project, Task task) {
		return workspaceDir(task.getName(), project.getRootDir());
	}

	/** Returns the workspace directory appropriate for the given name and file. */
	public File workspaceDir(String name, File ideDir) {
		return ownerToWorkspace.computeIfAbsent(ideDir, owner -> {
			File workspace = new File(root, name + "-" + owner.getAbsolutePath().hashCode());
			FileMisc.mkdirs(workspace);
			Errors.rethrow().run(() -> {
				FileMisc.writeToken(root, workspace.getName() + OWNER_PATH, ideDir.getAbsolutePath());
			});
			return workspace;
		});
	}

	/** Removes all workspace directories for which their owning workspace is no longer present. */
	public void clean() {
		Iterator<Map.Entry<File, File>> iter = ownerToWorkspace.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<File, File> entry = iter.next();
			File ownerDir = entry.getKey();
			File workspaceDir = entry.getValue();
			if (!ownerDir.exists()) {
				deleteWorkspace(workspaceDir, "owner " + ownerDir + " no longer exists.");
				iter.remove();
			}
		}
	}

	/** Tries to delete folder.  If it fails, it prints a warning but keeps going.  No reason to break a build over spilled diskspace. */
	private void deleteWorkspace(File workspace, String reason) {
		try {
			FileMisc.forceDelete(workspace);
			File token = new File(root, workspace.getName() + OWNER_PATH);
			FileMisc.forceDelete(token);
		} catch (Exception e) {
			System.err.println("Tried to delete workspace " + workspace.getAbsolutePath() + " because " + reason);
			e.printStackTrace();
		}
	}
}
