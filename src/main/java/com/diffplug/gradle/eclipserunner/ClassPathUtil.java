/*
 * Copyright (C) 2021 DiffPlug
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
package com.diffplug.gradle.eclipserunner;


import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ClassPathUtil {

	private final String installArea;

	public ClassPathUtil(String installArea) {
		this.installArea = installArea;
	}

	private static final String FILE_SCHEME = "file:"; //$NON-NLS-1$
	protected static final String REFERENCE_SCHEME = "reference:"; //$NON-NLS-1$

	public String searchFor(final String target) {
		return searchFor(target, installArea + "/plugins");
	}

	/**
	 *
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static URL[] getClasspath(ClassLoader classLoader) throws Exception {
		if (classLoader instanceof URLClassLoader) {
			return ((URLClassLoader) classLoader).getURLs();
		} else {
			// Assume AppClassLoader of Java9+
			Class<? extends ClassLoader> clz = classLoader.getClass();
			final Field ucpFld = clz.getDeclaredField("ucp");
			ucpFld.setAccessible(true);
			Object ucpObj = ucpFld.get(classLoader);
			final Field pathFld = ucpObj.getClass().getDeclaredField("path");
			pathFld.setAccessible(true);
			List<URL> pathObj = (List<URL>) pathFld.get(ucpObj);
			return pathObj.toArray(new URL[pathObj.size()]);
		}
	}

	/**
	 * Searches for the given target directory starting in the "plugins" subdirectory
	 * of the given location.
	 *
	 * @return the location where target directory was found, <code>null</code> otherwise
	 * @param start the location to begin searching
	 */
	public String searchFor(final String target, String start) {
		File root = resolveFile(new File(start));

		// Note that File.list only gives you file names not the complete path from start
		String[] candidates = root.list();
		if (candidates == null)
			return null;

		ArrayList<String> matches = new ArrayList<>(2);
		for (String candidate : candidates) {
			if (isMatchingCandidate(target, candidate, root)) {
				matches.add(candidate);
			}
		}
		String[] names = matches.toArray(new String[matches.size()]);
		int result = findMax(target, names);
		if (result == -1)
			return null;
		File candidate = new File(start, names[result]);
		return candidate.getAbsolutePath().replace(File.separatorChar, '/') + (candidate.isDirectory() ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
	}

	private boolean isMatchingCandidate(String target, String candidate, File root) {
		if (candidate.equals(target))
			return true;
		if (!candidate.startsWith(target + "_")) //$NON-NLS-1$
			return false;
		int targetLength = target.length();
		int lastUnderscore = candidate.lastIndexOf('_');

		//do we have a second '_', version (foo_1.0.0.v1_123) or id (foo.x86_64) ?
		//files are assumed to have an extension (zip or jar only), remove it
		//NOTE: we only remove .zip and .jar extensions because we still need to accept libraries with
		//simple versions (e.g. eclipse_1234.dll)
		File candidateFile = new File(root, candidate);
		if (candidateFile.isFile() && (candidate.endsWith(".jar") || candidate.endsWith(".zip"))) { //$NON-NLS-1$//$NON-NLS-2$
			int extension = candidate.lastIndexOf('.');
			candidate = candidate.substring(0, extension);
		}

		int lastDot = candidate.lastIndexOf('.');
		if (lastDot < targetLength) {
			// no dots after target, the '_' is not in a version (foo.x86_64 case), not a match
			return false;
		}

		//get past all '_' that are part of the qualifier
		while (lastUnderscore > lastDot)
			lastUnderscore = candidate.lastIndexOf('_', lastUnderscore - 1);

		if (lastUnderscore == targetLength)
			return true; //underscore at the end of target (foo_1.0.0.v1_123 case)
		return false; //another underscore between target and version (foo_64_1.0.0.v1_123 case)
	}

	public String searchForBundle(String target) {
		return searchForBundle(target, installArea + "/plugins");
	}

	public String searchForBundle(String target, String start) {
		//Only handle "reference:file:" urls, and not simple "file:" because we will be using the jar wherever it is.
		if (target.startsWith(REFERENCE_SCHEME)) {
			target = target.substring(REFERENCE_SCHEME.length());
			if (!target.startsWith(FILE_SCHEME))
				throw new IllegalArgumentException("Bundle URL is invalid: " + target); //$NON-NLS-1$
			target = target.substring(FILE_SCHEME.length());
			File child = new File(target);
			File fileLocation = child;
			if (!child.isAbsolute()) {
				File parent = resolveFile(new File(start));
				fileLocation = new File(parent, child.getPath());
			}
			return searchFor(fileLocation.getName(), fileLocation.getParentFile().getAbsolutePath());
		}
		return searchFor(target, start);
	}

	protected int findMax(String prefix, String[] candidates) {
		int result = -1;
		Object maxVersion = null;
		for (int i = 0; i < candidates.length; i++) {
			String name = (candidates[i] != null) ? candidates[i] : ""; //$NON-NLS-1$
			String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
			if (name.startsWith(prefix + "_")) //$NON-NLS-1$
				version = name.substring(prefix.length() + 1); //prefix_version
			Object currentVersion = getVersionElements(version);
			if (maxVersion == null) {
				result = i;
				maxVersion = currentVersion;
			} else {
				if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
					result = i;
					maxVersion = currentVersion;
				}
			}
		}
		return result;
	}

	/**
	 * Compares version strings.
	 * @return result of comparison, as integer;
	 * <code><0</code> if left < right;
	 * <code>0</code> if left == right;
	 * <code>>0</code> if left > right;
	 */
	private int compareVersion(Object[] left, Object[] right) {

		int result = ((Integer) left[0]).compareTo((Integer) right[0]); // compare major
		if (result != 0)
			return result;

		result = ((Integer) left[1]).compareTo((Integer) right[1]); // compare minor
		if (result != 0)
			return result;

		result = ((Integer) left[2]).compareTo((Integer) right[2]); // compare service
		if (result != 0)
			return result;

		return ((String) left[3]).compareTo((String) right[3]); // compare qualifier
	}

	/**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier). Note, that returning anything else will cause exceptions in the caller.
	 */
	private Object[] getVersionElements(String version) {
		if (version.endsWith(".jar")) //$NON-NLS-1$
			version = version.substring(0, version.length() - 4);
		Object[] result = {Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), ""}; //$NON-NLS-1$
		StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
		String token;
		int i = 0;
		while (t.hasMoreTokens() && i < 4) {
			token = t.nextToken();
			if (i < 3) {
				// major, minor or service ... numeric values
				try {
					result[i++] = Integer.valueOf(token);
				} catch (Exception e) {
					// invalid number format - use default numbers (0) for the rest
					break;
				}
			} else {
				// qualifier ... string value
				result[i++] = token;
			}
		}
		return result;
	}

	private URL buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		if (File.separatorChar == '\\')
			spec = spec.trim();
		boolean isFile = spec.startsWith(FILE_SCHEME);
		try {
			if (isFile) {
				File toAdjust = new File(spec.substring(5));
				toAdjust = resolveFile(toAdjust);
				if (toAdjust.isDirectory())
					return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
				return toAdjust.toURL();
			}
			return new URL(spec);
		} catch (MalformedURLException e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				File toAdjust = new File(spec);
				if (toAdjust.isDirectory())
					return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
				return toAdjust.toURL();
			} catch (MalformedURLException e1) {
				return null;
			}
		}
	}

	/**
	 * Resolve the given file against  osgi.install.area.
	 * If osgi.install.area is not set, or the file is not relative, then
	 * the file is returned as is.
	 */
	private File resolveFile(File toAdjust) {
		if (!toAdjust.isAbsolute()) {
			if (installArea != null) {
				if (installArea.startsWith(FILE_SCHEME))
					toAdjust = new File(installArea.substring(5), toAdjust.getPath());
				else if (new File(installArea).exists())
					toAdjust = new File(installArea, toAdjust.getPath());
			}
		}
		return toAdjust;
	}

	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getFile();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URL(url.getProtocol(), url.getHost(), file);
	}

}
