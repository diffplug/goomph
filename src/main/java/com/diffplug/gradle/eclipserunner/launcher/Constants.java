/*
 * Copyright (C) 2016-2021 DiffPlug
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
package com.diffplug.gradle.eclipserunner.launcher;

/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrew Niefer - IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Copied on 2021-02-11 straight from https://github.com/eclipse/rt.equinox.framework/blob/7433233fbcf44fe8b82e63a10f8733767e5b3042/bundles/org.eclipse.equinox.launcher/src/org/eclipse/equinox/internal/launcher/Constants.java
 */
public class Constants {
	public static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$
	public static final String INTERNAL_OS_SUNOS = "SunOS"; //$NON-NLS-1$
	public static final String INTERNAL_OS_LINUX = "Linux"; //$NON-NLS-1$
	public static final String INTERNAL_OS_MACOSX = "Mac OS"; //$NON-NLS-1$
	public static final String INTERNAL_OS_AIX = "AIX"; //$NON-NLS-1$
	public static final String INTERNAL_OS_HPUX = "HP-UX"; //$NON-NLS-1$
	public static final String INTERNAL_OS_QNX = "QNX"; //$NON-NLS-1$
	public static final String INTERNAL_OS_OS400 = "OS/400"; //$NON-NLS-1$
	public static final String INTERNAL_OS_OS390 = "OS/390"; //$NON-NLS-1$
	public static final String INTERNAL_OS_ZOS = "z/OS"; //$NON-NLS-1$

	public static final String ARCH_X86 = "x86";//$NON-NLS-1$
	public static final String ARCH_X86_64 = "x86_64";//$NON-NLS-1$

	/**
	 * Constant string (value "win32") indicating the platform is running on a
	 * Window 32-bit operating system (e.g., Windows 98, NT, 2000).
	 */
	public static final String OS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value "linux") indicating the platform is running on a
	 * Linux-based operating system.
	 */
	public static final String OS_LINUX = "linux";//$NON-NLS-1$

	/**
	 * Constant string (value "aix") indicating the platform is running on an
	 * AIX-based operating system.
	 */
	public static final String OS_AIX = "aix";//$NON-NLS-1$

	/**
	 * Constant string (value "solaris") indicating the platform is running on a
	 * Solaris-based operating system.
	 */
	public static final String OS_SOLARIS = "solaris";//$NON-NLS-1$

	/**
	 * Constant string (value "hpux") indicating the platform is running on an
	 * HP/UX-based operating system.
	 */
	public static final String OS_HPUX = "hpux";//$NON-NLS-1$

	/**
	 * Constant string (value "qnx") indicating the platform is running on a
	 * QNX-based operating system.
	 */
	public static final String OS_QNX = "qnx";//$NON-NLS-1$

	/**
	 * Constant string (value "macosx") indicating the platform is running on a
	 * Mac OS X operating system.
	 */
	public static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	/**
	 * Constant string (value "os/400") indicating the platform is running on a
	 * OS/400 operating system.
	 */
	public static final String OS_OS400 = "os/400"; //$NON-NLS-1$

	/**
	 * Constant string (value "os/390") indicating the platform is running on a
	 * OS/390 operating system.
	 */
	public static final String OS_OS390 = "os/390"; //$NON-NLS-1$

	/**
	 * Constant string (value "z/os") indicating the platform is running on a
	 * z/OS operating system.
	 */
	public static final String OS_ZOS = "z/os"; //$NON-NLS-1$

	/**
	 * Constant string (value "unknown") indicating the platform is running on a
	 * machine running an unknown operating system.
	 */
	public static final String OS_UNKNOWN = "unknown";//$NON-NLS-1$

	/**
	 * Constant string (value "win32") indicating the platform is running on a
	 * machine using the Windows windowing system.
	 */
	public static final String WS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value "wpf") indicating the platform is running on a
	 * machine using the Windows Presendation Foundation system.
	 */
	public static final String WS_WPF = "wpf";//$NON-NLS-1$

	/**
	 * Constant string (value "motif") indicating the platform is running on a
	 * machine using the Motif windowing system.
	 */
	public static final String WS_MOTIF = "motif";//$NON-NLS-1$

	/**
	 * Constant string (value "gtk") indicating the platform is running on a
	 * machine using the GTK windowing system.
	 */
	public static final String WS_GTK = "gtk";//$NON-NLS-1$

	/**
	 * Constant string (value "photon") indicating the platform is running on a
	 * machine using the Photon windowing system.
	 */
	public static final String WS_PHOTON = "photon";//$NON-NLS-1$

	/**
	 * Constant string (value "carbon") indicating the platform is running on a
	 * machine using the Carbon windowing system (Mac OS X).
	 */
	public static final String WS_CARBON = "carbon";//$NON-NLS-1$

	/**
	 * Constant string (value "cocoa") indicating the platform is running on a
	 * machine using the Cocoa windowing system (Mac OS X).
	 */
	public static final String WS_COCOA = "cocoa"; //$NON-NLS-1$

	/**
	 * Constant string (value "unknown") indicating the platform is running on a
	 * machine running an unknown windowing system.
	 */
	public static final String WS_UNKNOWN = "unknown";//$NON-NLS-1$
}
