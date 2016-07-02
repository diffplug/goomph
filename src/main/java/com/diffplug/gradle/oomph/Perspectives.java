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

/**
 * Listing of common eclipse perspectives (window layouts).
 * 
 * Useful for setting initial perspective {@link OomphBranding#perspective(String)}.
 */
public class Perspectives {
	private Perspectives() {}

	/** Requires {@link IUs#IDE}. */
	public static final String RESOURCES = "org.eclipse.ui.resourcePerspective";
	/** Requires {@link IUs#JDT}. */
	public static final String JAVA = "org.eclipse.jdt.ui.JavaPerspective";
	/** Requires {@link IUs#JDT}. */
	public static final String JAVA_HIERARCHY = "org.eclipse.jdt.ui.JavaHierarchyPerspective";
	/** Requires {@link IUs#JDT}. */
	public static final String JAVA_BROWSING = "org.eclipse.jdt.ui.JavaBrowsingPerspective";
	/** Requires {@link IUs#JDT}. */
	public static final String DEBUG = "org.eclipse.debug.ui.DebugPerspective";
	/** Requires {@link IUs#PDE}. */
	public static final String PDE = "org.eclipse.pde.ui.PDEPerspective";
}
