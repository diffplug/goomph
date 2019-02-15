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
package com.diffplug.gradle;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Optional;

import org.gradle.api.Project;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.gradle.p2.P2Model;

/**
 * There are a few things which goomph
 * needs to cache on the developer's machine.
 * They are described exhaustively by this
 * class.
 *
 * - {@link #p2bootstrap()}
 * - {@link #pdeBootstrap()}
 * - {@link #pdeBootstrapUrl()}
 * - {@link #bundlePool()}
 * - {@link #workspaces()}
 *
 * All these values can be overridden either by setting the
 * value of the `public static override_whatever` variable.
 * 
 * If your gradle build is split across multiple files using
 * `apply from:`, then these static variables will get wiped
 * out.  You can fix this by setting a project property
 * as such: `project.ext.goomph_override_whatever=something`
 */
@SuppressFBWarnings("MS_SHOULD_BE_FINAL")
public class GoomphCacheLocations {
	private static final String ROOT = ".goomph";

	/** Initializes overrides based on project properties named "goomph_override_whatever" */
	public static void initFromProject(Project project) {
		for (Field field : GoomphCacheLocations.class.getFields()) {
			Object value = project.getProperties().get("goomph_" + field.getName());
			if (value != null) {
				Errors.rethrow().run(() -> field.set(null, value));
			}
		}
	}

	/**
	 * {@link MavenCentralMap} needs to look at p2 metadata
	 * to know what the version numbers are for the specific
	 * bundles of a given eclipse release are.
	 * 
	 * Rather than downloading this metadata over and over, we only
	 * download it once, and cache the results here.
	 */
	public static File eclipseReleaseMetadata() {
		return defOverride(ROOT + "/eclipse-release-metadata", override_eclipseReleaseMetadata);
	}

	public static File override_eclipseReleaseMetadata = null;

	/**
	 * When Goomph creates an IDE for you, it must
	 * also create an eclipse workspace.  Unfortunately,
	 * that workspace cannot be a subdirectory of the 
	 * project directory (an eclipse limitation).  This is a
	 * problem for single-project builds.
	 * 
	 * As a workaround, we put all eclipse workspaces in a central
	 * location, which is tied to their project directory.  Whenever
	 * a new workspace is created, we do a quick check to make there
	 * aren't any stale workspaces.  If the workspace has gone stale,
	 * we delete it.
	 */
	public static File workspaces() {
		return defOverride(ROOT + "/ide-workspaces", override_workspaces);
	}

	public static File override_workspaces = null;

	/**
	 * Location where the p2-bootstrap application should be downloaded from.
	 *
	 * Goomph's p2 tasks rely on the eclipse [p2 director application](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html&cp=2_0_20_2).
	 * It is distributed within the Eclipse SDK.  But rather
	 * than requiring the entire SDK, we have packaged just
	 * the jars required for p2 director into a ~7MB download
	 * available on bintray as [goomph-p2-bootstrap](https://bintray.com/diffplug/opensource/goomph-p2-bootstrap/view).
	 *
	 * This only gets downloaded if you use {@link P2Model}.
	 * 
	 * Defaults to `https://dl.bintray.com/diffplug/opensource/com/diffplug/gradle/goomph-p2-bootstrap/`.  If you override, it still
	 * needs to follow the correct versioning scheme.  e.g. if you want to relocate to `http://intranet/goomph-p2-boostrap`, then
	 * the artifact will need to be available at `http://intranet/goomph-p2-boostrap/4.5.2/goomph-p2-bootstrap.zip`
	 *
	 * As new versions of p2bootstrap come out, you will have to update your internal URL cache, but these releases are infrequent.
	 */
	public static Optional<String> p2bootstrapUrl() {
		return Optional.ofNullable(override_p2bootstrapUrl);
	}

	public static String override_p2bootstrapUrl = null;

	/**
	 * Location where the p2-bootstrap application
	 * is cached: `~/.goomph/p2-bootstrap`.
	 *
	 * Goomph's p2 tasks rely on the eclipse [p2 director application](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html&cp=2_0_20_2).
	 * It is distributed within the Eclipse SDK.  But rather
	 * than requiring the entire SDK, we have packaged just
	 * the jars required for p2 director into a ~7MB download
	 * available on bintray as [goomph-p2-bootstrap](https://bintray.com/diffplug/opensource/goomph-p2-bootstrap/view).
	 *
	 * This only gets downloaded if you use {@link P2Model}.
	 */
	public static File p2bootstrap() {
		return defOverride(ROOT + "/p2-bootstrap", override_p2bootstrap);
	}

	public static File override_p2bootstrap = null;

	/**
	 * Location where the pde-bootstrap application should be downloaded from.
	 *
	 * Goomph's pde tasks rely on the eclipse [p2 director application](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fguide%2Fp2_director.html&cp=2_0_20_2).
	 * It is distributed within the Eclipse SDK.  The Package is
	 * downloaded and installed during configuration from official eclipse
	 * repository.
	 *
	 * Defaults to official update site of the selected Eclipse Release.  If you override, it still
	 * needs to follow the correct versioning scheme.  e.g. if you want to relocate to `http://intranet/goomph-pde-boostrap`, then
	 * the artifact will need to be available at `http://intranet/goomph-pde-boostrap/4.5.2/goomph-pde-bootstrap.zip`
	 *
	 * As new versions of pdeBootstrap come out, you will have to update your internal URL cache, but these releases are infrequent.
	 */
	public static Optional<String> pdeBootstrapUrl() {
		return Optional.ofNullable(override_pdeBootstrapUrl);
	}

	public static String override_pdeBootstrapUrl = null;

	/**
	 * Location where eclipse instances with PDE build
	 * are cached: `~/.goomph/pde-bootstrap`.
	 */
	public static File pdeBootstrap() {
		return defOverride(ROOT + "/pde-bootstrap", override_pdeBootstrap);
	}

	public static File override_pdeBootstrap = null;

	/**
	 * Bundle pool used for caching jars and
	 * assembling disjoint eclipse installs: `~/.p2/pool`
	 *
	 * If you are using [Oomph](https://projects.eclipse.org/projects/tools.oomph)
	 * to create lots of eclipse installations, then this will go a lot
	 * faster if you cache all of their jars in a central location.
	 *
	 * Oomph does this by default in the given location.
	 */
	public static File bundlePool() {
		//return defOverride(".p2/pool", override_bundlePool);
		return defOverride(ROOT + "/shared-bundles", override_bundlePool);
	}

	public static File override_bundlePool = null;

	private static File defOverride(String userHomeRelative, File override) {
		return Optional.ofNullable(override).orElseGet(() -> {
			return userHome().resolve(userHomeRelative).toFile();
		});
	}

	private static Path userHome() {
		return new File(StandardSystemProperty.USER_HOME.value()).toPath();
	}
}
