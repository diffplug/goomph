# Goomph releases

### Version 3.13.0-SNAPSHOT - TBD ([javadoc](http://diffplug.github.io/goomph/javadoc/snapshot/), [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/diffplug/gradle/goomph/))

- Added support for translating eclipse releases to maven central coordinates. [(#61)](https://github.com/diffplug/goomph/pull/61)
- Added support for eclipse 4.7.3. [(#62)](https://github.com/diffplug/goomph/pull/62)

### Version 3.12.0 - February 26th 2018 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.12.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.12.0/view))

- Added support for `includeLaunchers` property from product files [(#58)](https://github.com/diffplug/goomph/pull/58)

### Version 3.11.1 - February 22nd 2018 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.11.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.11.1/view))

- Bump OSGi version to fix `NoClassDefFoundError: org/eclipse/osgi/framework/util/CaseInsensitiveDictionaryMap` [(#57)](https://github.com/diffplug/goomph/pull/57)

### Version 3.11.0 - February 18th 2018 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.11.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.11.0/view))

- Bump pde-bootstrap from `4.5.2` to `4.7.2`.
- Allow fine grained configuration of EclipseApp in PdeBuildTask [(#55)](https://github.com/diffplug/goomph/pull/55)

### Version 3.10.0 - February 5th 2018 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.10.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.10.0/view))

- Added nosplash argument to EclipseApp in order to prevent splash screens during gradle tasks. [(#53)](https://github.com/diffplug/goomph/pull/53)
- Added a feature to provide a custom goomph-pde-bootstrap installation. [(#52)](https://github.com/diffplug/goomph/pull/52)
- Fix typo in `GOOMPH_PDE_UPDATE_SITE` property (was accidentally UDPATE). [(#48)](https://github.com/diffplug/goomph/pull/48)
	- Old spelling (UDPATE) is still supported for backward-compatibility.

### Version 3.9.1 - February 2nd 2018 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.9.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.9.1/view))

- Added `--launcher.suppressErrors` to all `EclipseApp` invocations so that build errors won't open a blocking dialog on build servers. [(#49)](https://github.com/diffplug/goomph/pull/49)
- Fixed a bug where a console app's execution might block because of an overfilled stderr. [(#50)](https://github.com/diffplug/goomph/pull/50)

### Version 3.9.0 - December 21st 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.9.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.9.0/view))

- Added `addBuildSrc()` method to Oomph configuration.
- Upgrade bndlib from `3.4.0` to `3.5.0`.
- `p2asmaven` now supports [slicing options](https://wiki.eclipse.org/Equinox/p2/Ant_Tasks#SlicingOptions). ([#41](https://github.com/diffplug/goomph/pull/41))
- `p2asmaven` now supports appending - a huge performance improvement for incrementally adding p2 deps. ([#44](https://github.com/diffplug/goomph/pull/44))
- Updated `EclipseRelease` to `4.7.2`.

### Version 3.8.1 - October 13th 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.8.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.8.1/view))

- Updated `EclipseRelease` to `4.7.1a`, and also added `4.7.1`.
- Updated default buildship release from `e46/releases/1.0` to `e47/releases/2.x`.

### Version 3.8.0 - September 21st 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.8.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.8.0/view))

- Added the [`com.diffplug.gradle.equinoxlaunch`](https://diffplug.github.io/goomph/javadoc/snapshot/com/diffplug/gradle/eclipserunner/EquinoxLaunchPlugin.html) can configure and run equinox applications as part of the build, such as a code generator.
- CopyJarsUsingProductFile now gives better error messages when a plugin is missing.
- Bump bndlib from `3.2.0` to `3.4.0`.

### Version 3.7.3 - July 4th 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.7.3/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.7.3/view))

- Updated `EclipseRelease.latestOfficial()` to `Oxygen`.

### Version 3.7.2 - April 13th 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.7.2/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.7.2/view))

- Updated `EclipseRelease.latestOfficial()` to `Neon.3`.
- Fixed a confusing error message ([#30](https://github.com/diffplug/goomph/issues/30)).

### Version 3.7.1 - February 14th 2017 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.7.1/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.7.1/view))

* `com.diffplug.gradle.eclipse.buildproperties` now includes all non-java files in the `src` folder as resources, instead of only `*.properties` files.

### Version 3.7.0 - December 22nd 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.7.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.7.0/view))

* Updated `EclipseResult.latestOfficial()` to `Neon.2`.
* It is now possible to set the description in the startup dialog and about dialog. [commit](https://github.com/diffplug/goomph/commit/f24ac1ba8d00731ba754f1ede70bd93d232f0b67)
* Fixed JDK detection on mac. [commit](https://github.com/diffplug/goomph/commit/d0555c8a483f29f9b8b39c05578a7ea9cc45253f)
* Goomph constants (such as p2 bootstrap url) can now be overridden using Gradle extension properties:
	+ e.g. `ext.goomph_override_p2bootstrapUrl='http://intranet/goomph-p2-boostrap'`
	+ Required when splitting buildscripts across files, because of Gradle classpath separation.
	+ See [issue #25](https://github.com/diffplug/goomph/issues/25) for more details.
* Fixed bug which prevented buildship import. [issue](https://github.com/diffplug/gradle-and-eclipse-rcp/issues/7)
* Added a mechanism for integrating third-party plugins into Goomph, e.g.

```gradle
oomphIde {
	thirdParty {
		tmTerminal {}
		minimalistGradleEditor {}
		buildship {}
	}
}
```

### Version 3.6.0 - December 5th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.6.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.6.0/view))

* Added `workspaceFile('pathInWorkspace', 'srcFile')` for copying files into the workspace directly.
* Improved `workspaceProp('pathInWorkspace', { map -> map.put('key', 'value')}` so it can now be called multiple times to modify earlier results, including modifying a file set by `workspaceFile`.  It can still create a file from scratch, as before.
* Added `workspaceXml('pathInWorkspace', { xmlProvider -> xmlProvider.asNode() ...})` which can modify an XML file which was initially created by `workpaceFile`.  It *cannot* create an xml file from scratch, however.
* `style { niceText() }` now sets line numbers, and there are methods which give more fine-grained control ([#20](https://github.com/diffplug/goomph/pull/20)).
* Added the ability to change how the oomphIde p2 action is carried out, using `runUsingPDE()` ([#19](https://github.com/diffplug/goomph/issues/19)).
* Improvements to the JDT config and the ability to link resources ([#23](https://github.com/diffplug/goomph/pull/23)).
* The p2bootstrap url can now be overridden ([#25](https://github.com/diffplug/goomph/issues/25)).

### Version 3.5.0 - November 24th 2016 ([javadoc](http://diffplug.github.io/goomph/javadoc/3.5.0/), [jcenter](https://bintray.com/diffplug/opensource/goomph/3.5.0/view))

* Added the ability to set the installed JRE (#16).
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
