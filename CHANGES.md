# Goomph releases

### Version 3.6.0 - TBD ([javadoc](http://diffplug.github.io/goomph/javadoc/snapshot/), [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/gradle/goomph/))

### Version 3.5.0 - November 24th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.5.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.5.0/view))

* Added the ability to set the installed JRE, thanks to @scottresnik.
	+ See [javadoc](http://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/oomph/ConventionJdt.html) for details.

### Version 3.4.0 - November 22nd 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.4.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.4.0/view))

* Added `FileMisc.deleteEmptyFolders().`
* Fixed `com.diffplug.gradle.eclipse.bndmanifest` so that it doesn't create `Export-Package` entries for empty packages.
	+ If bnd sees an empty folder in the classes directory, it will put that directory into the manifest.
	+ To fix this, we now clean empty folders out of the classes directory before we run bndmanifest.

### Version 3.3.0 - October 13th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.3.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.3.0/view))

* Added javadoc to [`AsMavenPlugin`](https://diffplug.github.io/goomph/javadoc/3.3.0/com/diffplug/gradle/p2/AsMavenPlugin.html) and [`OomphIdePlugin`](https://diffplug.github.io/goomph/javadoc/3.3.0/com/diffplug/gradle/oomph/OomphIdePlugin.html) which describes proxy support.
* Added `OomphIdeExtension::p2director`, to allow the proxy support described above.

### Version 3.2.1 - September 29th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.2.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.2.1/view))

* IDE setup tasks could not upgrade or downgrade the IDE version, because of a p2 director limitation. Fixed.

### Version 3.2.0 - September 28th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.2.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.2.0/view))

* `p2asmaven` now respects the buildDir variable ([#9](https://github.com/diffplug/goomph/issues/9)).
* `EclipseRelease` now knows about Neon.1, and uses Neon.1 as the latest available eclipse release.
* Improved `FeaturesAndBundlesPublisher` javadoc.
* P2BootstrapInstallation could fail intermittently due to filesystem flushing issues.  Fixed now.

### Version 3.1.2 - September 15th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.1.2/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.1.2/view))

* Second attempt at fixing a bug where trailing whitespaces in bundle symbolic names would break `p2asmaven`.

### Version 3.1.1 - September 14th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.1.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.1.1/view))

* Fixed a bug where trailing whitespaces in bundle symbolic names would break `p2asmaven`.

### Version 3.1.0 - August 24th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.1.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.1.0/view))

* Added `OomphIdeExtension.addProjectFolder()`.  You can now add projects to the eclipse IDE without having an eclipse project in the build.

### Version 3.0.6 - August 15th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.6/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.6/view))

* More fixes to behavior around creating the goomph bundle pool.
	+ Old behavior sometimes created a bundle pool which worked, but sometimes didn't.
	+ New behavior is more reliable.
* We were creating `file://` URLs on Windows, which should have been `file:///`.  This fix may cause some p2 repositories to become marked as dirty and redownload, even though they are really clean.  One-time fix.

### Version 3.0.5 - August 7th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.5/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.5/view))

* Fixed behavior around creating the goomph bundle pool.
	+ Old behavior was: if pool is listed as dependency but doesn't exist, just remove the dependency.
	+ New behavior is: if pool is listed as dependency but doesn't exist, create the pool.

### Version 3.0.4 - July 30th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.4/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.4/view))

* Fixed a mac-specific bug which made it impossible to use any PDE tasks.

### Version 3.0.3 - July 29th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.3/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.3/view))

* Fixed unnecessary errors for users who don't specify `org.gradle.java.home` manually.
* Fixed NPE when using `p2asmaven` on jars with no manifest.
	+ Failures on a single jar will no longer bring down the whole build.
	+ Jars with no manifest have their name and version parsed from their filename.

### Version 3.0.2 - July 15th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.2/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.2/view))

* Fixed `com.diffplug.gradle.eclipse.excludebuildfolder`.

### Version 3.0.1 - July 14th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.1/view))

* Fixed plugin metadata so that all our plugins make it to the plugin portal.  The following were missing:
	+ `com.diffplug.gradle.eclipse.resourcefilters`
	+ `com.diffplug.gradle.oomph.ide`

### Version 3.0.0 - July 13th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.0.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.0.0/view))

We added a ton of stuff in 3.0.0.  Everything in the `p2` and `pde` packages has been revamped, but the `eclipse`
and `osgi` packages are unchanged.

* BREAKING CHANGE: Everything in the `pde` package has been revamped.
	+ Wuff/Unpuzzle are no longer required.
* BREAKING CHANGE: Renamed ZipUtil to ZipMisc.
* BREAKING CHANGE: Renamed P2DirectorModel to P2Model.
* Added the ability to download a small (~10MB) archive with everything required to run p2 director and the p2 ant tasks.
* `ProjectDepsPlugin` can now replace binary dependencies with an eclipse project.
* Added `NativeFileManager` for opening the native file manager to look at a file (for debugging).
* Added `OomphIdePlugin`.

### Version 2.1.1 - May 9th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/2.1.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/2.1.1/view))

* `com.diffplug.gradle.osgi.bndmanifest` now always writes out the calculated manifest to the output resources directory.  This way, if a task such as `test` relies on having a valid manifest, it will be there.
* Note that if your tests rely on having an accurate META-INF/MANIFEST.MF, you should add `test.dependsOn(jar)` to your buildscript.

### Version 2.1.0 - May 9th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/2.1.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/2.1.0/view))

* `com.diffplug.gradle.osgi.bndmanifest` now has the ability to specify whether or not to merge the calculated manifest with the existing manifest.  Default behavior is don't merge.

### Version 2.0.0 - May 7th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/2.0.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/2.0.0/view))

* BREAKING CHANGE: Moved former contents of `com.diffplug.gradle.eclipse` into `com.diffplug.gradle.pde`.
* BREAKING CHANGE: Renamed plugin  `com.diffplug.gradle.swt` to `com.diffplug.gradle.swt.nativedeps`.
* Added plugin `com.diffplug.gradle.osgi.bndmanifest` which uses bnd to generate `MANIFEST.MF` and the entire jar, while respecting the result of gradle's resources directory.
* Added plugin `com.diffplug.gradle.eclipse.buildproperties` which uses the Eclipse PDE build.properties file as the single source of truth for controlling binary assets.
* Added plugin `com.diffplug.gradle.eclipse.projectdeps` which fixes some bugs with interproject dependencies.
* Added plugin `com.diffplug.gradle.eclipse.excludebuildfolder` which excludes the build folder from the eclipse project resources.
* All plugins are now tested by Gradle's testkit.  Some of the custom tasks in the `pde` package still need better coverage.

### Version 1.3.1 - April 6th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.3.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.3.1/view))

* Fixed EclipseWuff on OS X for the rest (not just binaries).

### Version 1.3.0 - March 14th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.3.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.3.0/view))

* Fixed location of OS X eclipse binaries for Eclipse Mars and later.
	+ Required adding a `getVersionOsgi()` method to EclipseWuff.

### Version 1.2.0 - March 8th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.2.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.2.0/view))

* Fixed a DiffPlug-specific constant in PdeProductBuildTask
* Added support for Mars SR2

### Version 1.1.0 - November 12th 2015 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.1.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.1.0/view))

* Added the 'com.diffplug.gradle.swt' plugin, which applies all Eclipse dependencies needed to use SWT and JFace.
* Added EnvMisc for getting environment variables with nice error messages for missing variables.

### Version 1.0.1 - November 12th 2015 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.0.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.0.1/view))

* Fixed a hardcoded version.  Yikes.

### Version 1.0.0 - October 12th 2015 ([javadoc](http://diffplug.github.io/goomph/javadoc/1.0.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/1.0.0/view))

* Throw it over the wall!
* We use it in production at DiffPlug, for whatever that's worth.
