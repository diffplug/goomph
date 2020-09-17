# Goomph releases

## [Unreleased]

## [3.25.0] - 2020-09-17
### Added
- Eclipse `4.17.0` aka `2020-09` ([new and noteworthy](https://www.eclipse.org/eclipse/news/4.17/)).
  - **WARNING** the `ide` task is currently broken with `4.17.0` (help wanted, [#129](https://github.com/diffplug/goomph/issues/129))
  - the `mavencentral` plugin still works great with `4.17.0`

## [3.24.0] - 2020-08-04
### Added
- Added `CategoryPublisher` ([#124](https://github.com/diffplug/goomph/issues/124))

## [3.23.0] - 2020-06-17
### Added
- Eclipse `4.16.0` aka `2020-06` ([new and noteworthy](https://www.eclipse.org/eclipse/news/4.16/)).
### Fixed
- Give a better error message when using `com.diffplug.eclipse.mavencentral` with an underspecified release. ([#121](https://github.com/diffplug/goomph/issues/121))
- Improve documentation for `com.diffplug.p2.asmaven` to better describe multiproject setups in Gradle 6+. ([#119](https://github.com/diffplug/goomph/issues/119))

## [3.22.0] - 2020-03-20
### Added
- Eclipse `4.15.0` aka `2020-03` ([new and noteworthy](https://www.eclipse.org/eclipse/news/4.15/)).
### Fixed
- Fix markdown formatting for the `AsMavenPlugin` javadocs ([#118](https://github.com/diffplug/goomph/pull/118)).
- Fix `java.lang.NoSuchMethodError: org.gradle.util.SingleMessageLogger.nagUserOfReplacedPlugin` error in gradle 6.2 ([details](https://github.com/diffplug/image-grinder/issues/5)).

## [3.21.0] - 2020-01-26
### Added
- New plugin `com.diffplug.eclipse.apt`, which modifies eclipse project files to support gradle's annotation processing.
  - It is a near-exact copy of [Thomas Broyer](https://github.com/tbroyer)'s excellent `net.ltgt.apt-eclipse` plugin.  The only change is that Gradle 6+ warnings have been fixed.  We will maintain it to address any future deprecations as well.

### Changed
- All plugin ids (not classes) have [moved, from `plugins { id 'com.diffplug.gradle.blah' }` to `com.diffplug.blah`](https://github.com/diffplug/goomph/pull/115/files#diff-503f218d646c10f484fdc9d6315bf2e3)) ([#115](https://github.com/diffplug/goomph/pull/115))
  - The old ids will keep working, but they'll print a warning advising a switch to the new id.
  - One exception: `com.diffplug.gradle.equinoxlaunch` -> `com.diffplug.osgi.equinoxlaunch` (it should have been in OSGi category all along)
  - This is annoying, but the deprecation warnings include an easy regex fix, and it's better to fix this now than later.
  - [More detail on the reasoning](https://dev.to/nedtwigg/names-in-java-maven-and-gradle-2fm2) if you're curious.
- Bump `bndlib` from `4.2.0` to `5.0.0`.
- Deprecate the `com.diffplug.swt.nativedeps` plugin, because `com.diffplug.eclipse.mavencentral` is now a better option.

## [3.20.0] - 2020-01-11
### Added
- Added eclipse `4.14.0` aka `2019-12` ([new and noteworthy](https://www.eclipse.org/eclipse/news/4.14/)).
- `EquinoxLaunchTask` now has up-to-date support if you manually specify output files, as well as any program-specific input files
- Added `MavenCentralExtesion.testImplementation` and deprecated `testCompile` (fixes [#109](https://github.com/diffplug/goomph/issues/109)).
### Changed
- Simplified build with [blowdryer](https://github.com/diffplug/blowdryer).
- Updated gradle and all plugins.
### Fixed
- `p2asmaven` now works with Gradle 6+ (fixes [#106](https://github.com/diffplug/goomph/issues/106))
- broken javadoc links (fixes [#105](https://github.com/diffplug/goomph/issues/105))
- missing annotation warnings on `PdeBuildTask` (still no up-to-date support though)
- `oomphIde` `workspaceXml` would wipe out the existing XML (fixes issue found in [#85](https://github.com/diffplug/goomph/issues/85)).
- if the p2 bootstrap download fails, it will now complain loudly rather than write the text `404` to a file (fixes [#101](https://github.com/diffplug/goomph/issues/101)).

## [3.19.0] - 2020-01-11 [YANKED]

This was published accidentally.  Nothing bad in it, it's just a halfway-done version of `3.20.0` above.

## [3.18.1] - 2019-09-20
- Added eclipse `4.13.0` aka `2019-09` ([new and noteworthy](https://www.eclipse.org/eclipse/news/4.13/)).
- Removed the `com.diffplug.gradle.eclipse.classic` plugin, because it does not work well with newer versions of gradle or eclipse.

## [3.18.0] - 2019-08-12
- Minimum gradle version is now `5.1`
- Migrated off of all deprecated APIs as of Gradle `5.5.1`.
- Fix URL for the `minimalistGradleEditor()` update site.
- Fix bug in `com.diffplug.gradle.eclipse.projectdeps` where libraries which were a prefix of another library could screw up replacement, e.g. `durian-swt` and `durian-swt.os`.

## [3.17.7] - 2019-06-26
- Added eclipse `4.12.0` aka `2019-06`.

## [3.17.6] - 2019-03-21
- Adopted `okhttp` so that we respect redirects when downloading.  Fixes bugs introduced when `download.eclipse.org` began using redirects.

## [3.17.5] - 2019-03-20
- Added eclipse `4.11.0` aka `2019-03`.

## [3.17.4] - 2019-02-15
- Replace `http://` with `https://` throughout the project, now that [eclipse supports https for download.eclipse.org](https://bugs.eclipse.org/bugs/show_bug.cgi?id=444350) ([#94](https://github.com/diffplug/goomph/pull/94)).

## [3.17.3] - 2019-02-06
- Fixed up-to-date checking for the `BndManifestPlugin` ([6480298](https://github.com/diffplug/goomph/commit/6480298173988656fa29035c6533fac39ceedfa4)).

## [3.17.2] - 2019-02-04
- Fix the `equinoxLaunch` plugin, and added a test to prevent future breakage. ([#93](https://github.com/diffplug/goomph/pull/93))

## [3.17.1] - 2019-01-02
- Added eclipse `4.10.0`.

## [3.17.0] - 2018-10-03
- Generated manifest is now put into the output resources directory, to make sure that it's available at runtime for development.
- Fixed a bug in ProjectDepsPlugin where similarly-named jars might not replace all of the desired projects.
  + e.g. if you want to replace `durian-swt`, `durian-swt.os`, and `durian-swt.cocoa.macosx.x86_64`, in the old version `durian-swt` would not get replaced.  Now fixed. ([#80](https://github.com/diffplug/goomph/pull/80))
- Add BndManifest support for every Jar task. [(#79)](https://github.com/diffplug/goomph/pull/79)
- Added eclipse `4.9.0`. ([#83](https://github.com/diffplug/goomph/pull/83))

## [3.16.0] - 2018-08-01
- Added ability to set vmArgs for EquinoxLaunchTask.
- Add `4.7.3.a` as an EclipseRelease.
- Update minimum gradle version to `4.0`:
  + `eclipseMavenCentral` now supports the new configurations: `api`, `implementation`, `runtimeOnly`, `testRuntimeOnly`.
  + `osgiBndManifest` no longer uses deprecated methods.
- Updated `bndlib` to `4.0`.
- Replaced dependency on `com.diffplug.durian:durian-swt` with `durian-swt.os`. This removes the dependency on `org.eclipse.swt`.
- Set the `oomphIde` splash location with an absolute path to fix warnings on mac. ([#74](https://github.com/diffplug/goomph/issues/74))

## [3.15.0] - 2018-07-06
- Added support for eclipse 4.8.0.

## [3.14.0] - 2018-05-18
- EquinoxLaunch handles applications with async exist code (EXIT_ASYNC_RESULT) [(#66)](https://github.com/diffplug/goomph/pull/66)
- Added `useNativesForRunningPlatform()` to `eclipseMavenCentral`.

## [3.13.0] - 2018-03-20
- Added support for translating eclipse releases to maven central coordinates. [(#61)](https://github.com/diffplug/goomph/pull/61)
- Added support for eclipse 4.7.3. [(#62)](https://github.com/diffplug/goomph/pull/62)

## [3.12.0] - 2018-02-26
- Added support for `includeLaunchers` property from product files [(#58)](https://github.com/diffplug/goomph/pull/58)

## [3.11.1] - 2018-02-22
- Bump OSGi version to fix `NoClassDefFoundError: org/eclipse/osgi/framework/util/CaseInsensitiveDictionaryMap` [(#57)](https://github.com/diffplug/goomph/pull/57)

## [3.11.0] - 2018-02-18
- Bump pde-bootstrap from `4.5.2` to `4.7.2`.
- Allow fine grained configuration of EclipseApp in PdeBuildTask [(#55)](https://github.com/diffplug/goomph/pull/55)

## [3.10.0] - 2018-02-05
- Added nosplash argument to EclipseApp in order to prevent splash screens during gradle tasks. [(#53)](https://github.com/diffplug/goomph/pull/53)
- Added a feature to provide a custom goomph-pde-bootstrap installation. [(#52)](https://github.com/diffplug/goomph/pull/52)
- Fix typo in `GOOMPH_PDE_UPDATE_SITE` property (was accidentally UDPATE). [(#48)](https://github.com/diffplug/goomph/pull/48)
 ] - Old spelling (UDPATE) is still supported for backward-compatibility.

## [3.9.1] - 2018-02-02
- Added `--launcher.suppressErrors` to all `EclipseApp` invocations so that build errors won't open a blocking dialog on build servers. [(#49)](https://github.com/diffplug/goomph/pull/49)
- Fixed a bug where a console app's execution might block because of an overfilled stderr. [(#50)](https://github.com/diffplug/goomph/pull/50)

## [3.9.0] - 2017-12-21
- Added `addBuildSrc()` method to Oomph configuration.
- Upgrade bndlib from `3.4.0` to `3.5.0`.
- `p2asmaven` now supports [slicing options](https://wiki.eclipse.org/Equinox/p2/Ant_Tasks#SlicingOptions). ([#41](https://github.com/diffplug/goomph/pull/41))
- `p2asmaven` now supports appending] - a huge performance improvement for incrementally adding p2 deps. ([#44](https://github.com/diffplug/goomph/pull/44))
- Updated `EclipseRelease` to `4.7.2`.

## [3.8.1] - 2017-10-13
- Updated `EclipseRelease` to `4.7.1a`, and also added `4.7.1`.
- Updated default buildship release from `e46/releases/1.0` to `e47/releases/2.x`.

## [3.8.0] - 2017-09-21
- Added the [`com.diffplug.gradle.equinoxlaunch`](https://javadoc.io/static/com.diffplug.gradle/goomph/snapshot/com/diffplug/gradle/eclipserunner/EquinoxLaunchPlugin.html) can configure and run equinox applications as part of the build, such as a code generator.
- CopyJarsUsingProductFile now gives better error messages when a plugin is missing.
- Bump bndlib from `3.2.0` to `3.4.0`.

<!-- END CHANGELOG -->

## [3.7.3] - July 4th 2017
- Updated `EclipseRelease.latestOfficial()` to `Oxygen`.

## [3.7.2] - April 13th 2017
- Updated `EclipseRelease.latestOfficial()` to `Neon.3`.
- Fixed a confusing error message ([#30](https://github.com/diffplug/goomph/issues/30)).

## [3.7.1] - February 14th 2017
* `com.diffplug.gradle.eclipse.buildproperties` now includes all non-java files in the `src` folder as resources, instead of only `*.properties` files.

## [3.7.0] - December 22nd 2016
* Updated `EclipseResult.latestOfficial()` to `Neon.2`.
* It is now possible to set the description in the startup dialog and about dialog. [commit](https://github.com/diffplug/goomph/commit/f24ac1ba8d00731ba754f1ede70bd93d232f0b67)
* Fixed JDK detection on mac. [commit](https://github.com/diffplug/goomph/commit/d0555c8a483f29f9b8b39c05578a7ea9cc45253f)
* Goomph constants (such as p2 bootstrap url) can now be overridden using Gradle extension properties:
  + e.g. `ext.goomph_override_p2bootstrapUrl='https://intranet/goomph-p2-boostrap'`
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

## [3.6.0] - December 5th 2016
* Added `workspaceFile('pathInWorkspace', 'srcFile')` for copying files into the workspace directly.
* Improved `workspaceProp('pathInWorkspace', { map -> map.put('key', 'value')}` so it can now be called multiple times to modify earlier results, including modifying a file set by `workspaceFile`.  It can still create a file from scratch, as before.
* Added `workspaceXml('pathInWorkspace', { xmlProvider -> xmlProvider.asNode() ...})` which can modify an XML file which was initially created by `workpaceFile`.  It *cannot* create an xml file from scratch, however.
* `style { niceText() }` now sets line numbers, and there are methods which give more fine-grained control ([#20](https://github.com/diffplug/goomph/pull/20)).
* Added the ability to change how the oomphIde p2 action is carried out, using `runUsingPDE()` ([#19](https://github.com/diffplug/goomph/issues/19)).
* Improvements to the JDT config and the ability to link resources ([#23](https://github.com/diffplug/goomph/pull/23)).
* The p2bootstrap url can now be overridden ([#25](https://github.com/diffplug/goomph/issues/25)).

## [3.5.0] - November 24th 2016
* Added the ability to set the installed JRE (#16).
  + See [javadoc](https://javadoc.io/static/com.diffplug.gradle/goomph/3.5.0/com/diffplug/gradle/oomph/ConventionJdt.html) for details.

## [3.4.0] - November 22nd 2016
* Added `FileMisc.deleteEmptyFolders().`
* Fixed `com.diffplug.gradle.eclipse.bndmanifest` so that it doesn't create `Export-Package` entries for empty packages.
  + If bnd sees an empty folder in the classes directory, it will put that directory into the manifest.
  + To fix this, we now clean empty folders out of the classes directory before we run bndmanifest.

## [3.3.0] - October 13th 2016
* Added javadoc to [`AsMavenPlugin`](https://javadoc.io/static/com.diffplug.gradle/goomph/3.3.0/com/diffplug/gradle/p2/AsMavenPlugin.html) and [`OomphIdePlugin`](https://javadoc.io/static/com.diffplug.gradle/goomph/3.3.0/com/diffplug/gradle/oomph/OomphIdePlugin.html) which describes proxy support.
* Added `OomphIdeExtension::p2director`, to allow the proxy support described above.

## [3.2.1] - September 29th 2016
* IDE setup tasks could not upgrade or downgrade the IDE version, because of a p2 director limitation. Fixed.

## [3.2.0] - September 28th 2016
* `p2asmaven` now respects the buildDir variable ([#9](https://github.com/diffplug/goomph/issues/9)).
* `EclipseRelease` now knows about Neon.1, and uses Neon.1 as the latest available eclipse release.
* Improved `FeaturesAndBundlesPublisher` javadoc.
* P2BootstrapInstallation could fail intermittently due to filesystem flushing issues.  Fixed now.

## [3.1.2] - September 15th 2016
* Second attempt at fixing a bug where trailing whitespaces in bundle symbolic names would break `p2asmaven`.

## [3.1.1] - September 14th 2016
* Fixed a bug where trailing whitespaces in bundle symbolic names would break `p2asmaven`.

## [3.1.0] - August 24th 2016
* Added `OomphIdeExtension.addProjectFolder()`.  You can now add projects to the eclipse IDE without having an eclipse project in the build.

## [3.0.6] - August 15th 2016
* More fixes to behavior around creating the goomph bundle pool.
  + Old behavior sometimes created a bundle pool which worked, but sometimes didn't.
  + New behavior is more reliable.
* We were creating `file://` URLs on Windows, which should have been `file:///`.  This fix may cause some p2 repositories to become marked as dirty and redownload, even though they are really clean.  One-time fix.

## [3.0.5] - August 7th 2016
* Fixed behavior around creating the goomph bundle pool.
  + Old behavior was: if pool is listed as dependency but doesn't exist, just remove the dependency.
  + New behavior is: if pool is listed as dependency but doesn't exist, create the pool.

## [3.0.4] - July 30th 2016
* Fixed a mac-specific bug which made it impossible to use any PDE tasks.

## [3.0.3] - July 29th 2016
* Fixed unnecessary errors for users who don't specify `org.gradle.java.home` manually.
* Fixed NPE when using `p2asmaven` on jars with no manifest.
  + Failures on a single jar will no longer bring down the whole build.
  + Jars with no manifest have their name and version parsed from their filename.

## [3.0.2] - July 15th 2016
* Fixed `com.diffplug.gradle.eclipse.excludebuildfolder`.

## [3.0.1] - July 14th 2016
* Fixed plugin metadata so that all our plugins make it to the plugin portal.  The following were missing:
  + `com.diffplug.gradle.eclipse.resourcefilters`
  + `com.diffplug.gradle.oomph.ide`

## [3.0.0] - July 13th 2016
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

## [2.1.1] - May 9th 2016
* `com.diffplug.gradle.osgi.bndmanifest` now always writes out the calculated manifest to the output resources directory.  This way, if a task such as `test` relies on having a valid manifest, it will be there.
* Note that if your tests rely on having an accurate META-INF/MANIFEST.MF, you should add `test.dependsOn(jar)` to your buildscript.

## [2.1.0] - May 9th 2016
* `com.diffplug.gradle.osgi.bndmanifest` now has the ability to specify whether or not to merge the calculated manifest with the existing manifest.  Default behavior is don't merge.

## [2.0.0] - May 7th 2016
* BREAKING CHANGE: Moved former contents of `com.diffplug.gradle.eclipse` into `com.diffplug.gradle.pde`.
* BREAKING CHANGE: Renamed plugin  `com.diffplug.gradle.swt` to `com.diffplug.gradle.swt.nativedeps`.
* Added plugin `com.diffplug.gradle.osgi.bndmanifest` which uses bnd to generate `MANIFEST.MF` and the entire jar, while respecting the result of gradle's resources directory.
* Added plugin `com.diffplug.gradle.eclipse.buildproperties` which uses the Eclipse PDE build.properties file as the single source of truth for controlling binary assets.
* Added plugin `com.diffplug.gradle.eclipse.projectdeps` which fixes some bugs with interproject dependencies.
* Added plugin `com.diffplug.gradle.eclipse.excludebuildfolder` which excludes the build folder from the eclipse project resources.
* All plugins are now tested by Gradle's testkit.  Some of the custom tasks in the `pde` package still need better coverage.

## [1.3.1] - April 6th 2016
* Fixed EclipseWuff on OS X for the rest (not just binaries).

## [1.3.0] - March 14th 2016
* Fixed location of OS X eclipse binaries for Eclipse Mars and later.
  + Required adding a `getVersionOsgi()` method to EclipseWuff.

## [1.2.0] - March 8th 2016
* Fixed a DiffPlug-specific constant in PdeProductBuildTask
* Added support for Mars SR2

## [1.1.0] - November 12th 2015
* Added the 'com.diffplug.gradle.swt' plugin, which applies all Eclipse dependencies needed to use SWT and JFace.
* Added EnvMisc for getting environment variables with nice error messages for missing variables.

## [1.0.1] - November 12th 2015
* Fixed a hardcoded version.  Yikes.

## [1.0.0] - October 12th 2015
* Throw it over the wall!
* We use it in production at DiffPlug, for whatever that's worth.
