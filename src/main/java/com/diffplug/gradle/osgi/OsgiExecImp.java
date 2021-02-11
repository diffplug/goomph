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
package com.diffplug.gradle.osgi;


import com.diffplug.gradle.FileMisc;
import com.diffplug.gradle.JavaExecWinFriendly;
import com.diffplug.gradle.SerializableMisc;
import com.diffplug.gradle.ZipMisc;
import com.diffplug.gradle.eclipserunner.ClassPathUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.jar.Manifest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

class OsgiExecImp {
	/** The name of this bundle, which contains the osgiembed package. */
	private static final String BUNDLE_SYMBOLIC_NAME = "com.diffplug.gradle.goomph";

	/** Returns this bundle within the given OSGi context, initializing it if necessary. */
	static Bundle loadBundle(BundleContext context) throws Exception {
		// look within the existing bundles
		for (Bundle bundle : context.getBundles()) {
			if (OsgiExecImp.BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}
		// look for our jar on the URLClassLoader path
		URL[] urls = ClassPathUtil.getClasspath(OsgiExecImp.class.getClassLoader());
		for (URL url : urls) {
			String name = url.getFile();
			if (name != null) {
				if (name.contains("/goomph")) {
					return context.installBundle(FileMisc.asUrl(new File(name)));
				} else if (name.contains("/" + JavaExecWinFriendly.LONG_CLASSPATH_JAR_PREFIX)) {
					// we're running with JavaExecWinFriendly, so we've gotta parse its classpath
					String content = ZipMisc.read(new File(name), "META-INF/MANIFEST.MF");
					try (InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
						Manifest manifest = new Manifest(input);
						String classpath = manifest.getMainAttributes().getValue("Class-Path");
						for (String piece : classpath.split(" ")) {
							if (piece.contains("/goomph")) {
								return context.installBundle(piece);
							}
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("Unable to find goomph jar");
	}

	public static <T extends OsgiExecable> void execInternal(File tempFile) throws Throwable {
		T object = SerializableMisc.read(tempFile);
		object.run();
		SerializableMisc.write(tempFile, object);
	}
}
