# Goomph releases

### Version 1.4.0 - TBD ([javadoc](http://diffplug.github.io/goomph/javadoc/snapshot/)) ([snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/gradle/goomph/))

* Added plugin `com.diffplug.gradle.eclipse.buildproperties` which uses the Eclipse PDE build.properties file as the single source of truth for controlling binary assets.
* Added plugin `com.diffplug.gradle.eclipse.projectdeps` which fixes some bugs with interproject dependencies.
* Added plugin `com.diffplug.gradle.eclipse.excludebuildfolder` which excludes the build folder from the eclipse project resources.
* Minor fix to SwtPlugin (now applied via `beforeEvaluate` rather than `afterEvaluate`, as appropriate for a plugin which changes dependencies)

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
