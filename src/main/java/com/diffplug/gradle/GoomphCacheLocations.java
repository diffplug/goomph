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
package com.diffplug.gradle;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
 *
 */
@SuppressFBWarnings("MS_SHOULD_BE_FINAL")
public class GoomphCacheLocations {
	private static final String ROOT = ".goomph";

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
