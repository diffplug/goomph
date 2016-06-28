# Goomph releases

### Version 3.0.0 - TBD ([javadoc](http://diffplug.github.io/goomph/javadoc/snapshot/)) ([snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/gradle/goomph/))

* BREAKING CHANGE: Everything in the `pde` package has been revamped.
	+ Wuff/Unpuzzle are no longer required.
* Added the ability to download a small (~10MB) archive with everything required to run p2 director and the p2 ant tasks.
* Added `

* `ProjectDepsPlugin` can now replace binary dependencies with an eclipse project.
* Added `NativeFileManager` for opening the native file manager to look at a file (for debugging).
* Added a lot of functionality to `ZipUtil` and `FileMisc`.

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
