# <img align="left" src="images/goomph_logo.png"> Goomph: IDE as build artifact
***Also plugins for working with SWT, OSGi, p2, and Eclipse RCP***

<!---freshmark shields
output = [
  link(shield('Gradle plugin', 'plugins.gradle.org', 'yes', 'blue'), 'https://plugins.gradle.org/search?term=goomph'),
  link(shield('Maven artifact', 'mavenCentral', 'com.diffplug.gradle:goomph', 'blue'), 'https://search.maven.org/artifact/com.diffplug.gradle/goomph'),
  link(shield('License Apache', 'license', 'Apache', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
  '',
  link(shield('Changelog', 'changelog', '{{versionLast}}', 'brightgreen'), 'CHANGES.md'),
  link(shield('Javadoc', 'javadoc', 'yes', 'brightgreen'), 'https://javadoc.io/doc/com.diffplug.gradle/goomph/{{versionLast}}/index.html'),
  link(shield('Live chat', 'gitter', 'live chat', 'brightgreen'), 'https://gitter.im/diffplug/goomph'),
  link(image('CircleCI', 'https://circleci.com/gh/diffplug/goomph.svg?style=shield'), 'https://circleci.com/gh/diffplug/goomph')
].join('\n');
-->
[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-yes-blue.svg)](https://plugins.gradle.org/search?term=goomph)
[![Maven artifact](https://img.shields.io/badge/mavenCentral-com.diffplug.gradle%3Agoomph-blue.svg)](https://search.maven.org/artifact/com.diffplug.gradle/goomph)
[![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-3.39.0-brightgreen.svg)](CHANGES.md)
[![Javadoc](https://img.shields.io/badge/javadoc-yes-brightgreen.svg)](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/index.html)
[![Live chat](https://img.shields.io/badge/gitter-live_chat-brightgreen.svg)](https://gitter.im/diffplug/goomph)
[![CircleCI](https://circleci.com/gh/diffplug/goomph.svg?style=shield)](https://circleci.com/gh/diffplug/goomph)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/doc/com.diffplug.gradle/goomph/', '/', versionLast);
-->

## Blog posts

- [P2, Maven, and Gradle](https://discuss.diffplug.com/t/p2-maven-and-gradle)
- [Parting out eclipse](https://discuss.diffplug.com/t/parting-out-eclipse)
- [Gradle and Eclipse RCP (youtube talk 2016)](https://www.youtube.com/watch?v=PIC6YeRkRlo&feature=youtu.be)

## Building OSGi bundles, Eclipse plugins, and RCP applications.

It turns out that the tooling required to implement "IDE-as-build-artifact" is the same tooling required to manipulate eclipse project files, and also to build Eclipse plugins and RCP applications in the first place.  That is Goomph's other side.  For an example project, see the [Gradle and Eclipse RCP talk](https://github.com/diffplug/gradle_and_eclipse_rcp).

Real world Eclipse software built with Goomph:
- [DiffPlug](https://www.diffplug.com/)
- [Veriluma](https://veriluma.com/)
- [GenStar](https://github.com/ANRGenstar/genstar)
- [WALA](https://github.com/wala/WALA)
- (your project here)

Below is an index of Goomph's capabilities, along with links to the javadoc where you can find usage examples.

#### `com.diffplug.eclipse` Eclipse project files and eclipse version-mapping maven central artifacts.

* [`apt`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/apt/AptEclipsePlugin.html) fixes eclipse project to work with Gradle annotation processing.
* [`mavencentral`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/MavenCentralPlugin.html) makes it easy to add dependency jars from an eclipse release.
* [`buildproperties`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/BuildPropertiesPlugin.html) uses [`build.properties`](https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm) to control a gradle build, and fixes eclipse project classpath to include binary assets specified in `build.properties`.
* [`excludebuildfolder`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/ExcludeBuildFolderPlugin.html) excludes the gradle `build` folder from Eclipse's resource indexing.
* [`projectdeps`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/ProjectDepsPlugin.html) fixes an intermittent problem where dependencies on other projects within the workspace aren't always resolved correctly within Eclipse.
* [`resourcefilters`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipse/ResourceFiltersPlugin.html) adds resource filters to the eclipse project.

#### `com.diffplug.osgi` Plugins for working with OSGi.

* [`bndmanifest`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/osgi/BndManifestPlugin.html) generates a manifest using purely bnd, and outputs it for IDE consumption.
* [`equinoxlaunch`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/eclipserunner/EquinoxLaunchPlugin.html) can configure and run equinox applications as part of the build, such as a code generator.
* [`OsgiExecable`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/osgi/OsgiExecable.html) makes it easy to run a chunk of code within an OSGi container, and get the result from outside the container.

#### `com.diffplug.p2` A  and plugins for manipulating p2 data. (*mostly [abandoned](https://github.com/diffplug/goomph/issues/166#issuecomment-945188596) now*)

* [`asmaven`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/AsMavenPlugin.html) downloads dependencies from a p2 repository and makes them available in a local maven repository.
* [`P2Model`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/P2Model.html) models a set of p2 repositories and IUs, and provides convenience methods for running p2-director or the p2.mirror ant task against these.
* [`P2AntRunner`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/P2AntRunner.html) runs eclipse ant tasks.
* [`CategoryPublisher`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/CategoryPublisher.html) models the CategoryPublisher eclipse application.
* [`FeaturesAndBundlesPublisher`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/FeaturesAndBundlesPublisher.html) models the FeaturesAndBundlesPublisher eclipse application.
* [`Repo2Runnable`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/p2/Repo2Runnable.html) models the Repo2Runnable eclipse application.

#### `com.diffplug.gradle.pde` Tasks for running Eclipse PDE using a downloaded eclipse instance. (*this part is mostly [abandoned](https://github.com/diffplug/goomph/issues/166#issuecomment-945188596) now*)

* [`PdeBuildTask`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/pde/PdeBuildTask.html) runs PDE build to build an RCP product.
* [`PdeAntBuildTask`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/pde/PdeAntBuildTask.html) runs PDE on an ant file.

#### `com.diffplug.gradle` Miscellaneous infrastructure.

* [`CmdLineTask`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/CmdLineTask.html) runs a series of shell commands, possibly copying or moving files in the meantime.
* [`JavaExecable`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/JavaExecable.html) makes it easy to run a chunk of code in a separate JVM, and get the result back in this one.
* [`JavaExecWinFriendly`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/JavaExecWinFriendly.html) overcomes limitations in Windows' commandline length and long classpaths.

#### `com.diffplug.gradle.eclipserunner` Infrastructure for running headless eclipse applications.

* Used to power the infrastructure above.

#### Other

* [`com.diffplug.configuration-cache-for-platform-specific-build`](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.39.0/com/diffplug/gradle/swt/PlatformSpecificBuildPlugin.html) allows you to use `OS.getNative()` and `OS.getRunning()` in your gradle build without breaking the configuration cache.

## IDE-as-build-artifact. (*this part is mostly [abandoned](https://github.com/diffplug/goomph/issues/189)*)

It is possible to have many installations of the Eclipse IDE share a common set of installed artifacts, called a "bundlepool".  This means it is fast and efficient to get a purpose-built IDE for every project, preconfigured with all the plugins and settings appropriate for the project at hand.

When you run `gradlew ide`, it builds and downloads an IDE into `build/oomphIde` with just the features you need.  Takes ~15 seconds and 1MB of disk space once all the common artifacts have been cached at `~/.goomph`.

```groovy
apply plugin: 'com.diffplug.oomph.ide'
oomphIde {
  repoEclipseLatest()
  jdt {}
  eclipseIni {
    vmargs('-Xmx2g')    // IDE can have up to 2 gigs of RAM
  }
  style {
    classicTheme()  // oldschool cool
    niceText()      // with nice fonts and visible whitespace
  }
}
```

See the [plugin's javadoc](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.37.0/com/diffplug/gradle/oomph/OomphIdePlugin.html) for a quickstart, and [HOW_TO_AUTOMATE_IDE.md](HOW_TO_AUTOMATE_IDE.md) for examples and more in-depth details.

<!---freshmark /javadoc -->

## Acknowledgements

* Thanks to [Jérémie Bresson](https://github.com/jmini) for [multiple](https://github.com/diffplug/goomph/pull/156) [fixes](https://github.com/diffplug/goomph/pull/158) to the `eclipseMavenCentral` plugin.
* Thanks to [Thomas Broyer](https://github.com/tbroyer) for the excellent [`net.ltgt.apt-eclipse`](https://github.com/tbroyer/gradle-apt-plugin) plugin.
  * It is no longer maintained at its original home, so we will continue to maintain it here at `com.diffplug.eclipse.apt`.
* Thanks to [ralfgrossklaus](https://github.com/ralfgrossklaus) for fixes to [Gradle 6.7](https://github.com/diffplug/goomph/pull/147), [CmdLine hanging](https://github.com/diffplug/goomph/pull/50), and [opening dialogs on buildservers](https://github.com/diffplug/goomph/pull/49).
* Thanks to [Kelvin Glaß](https://github.com/m273d15) for adding the `CategoryPublisher` application ([#126](https://github.com/diffplug/goomph/pull/126)).
* Thanks to [hacki11](https://github.com/hacki11) for [slicingOptions](https://github.com/diffplug/goomph/pull/41) and [append](https://github.com/diffplug/goomph/pull/44) in p2asmaven, as well as every improvement in the `3.10.0` release.
* Andrey Hihlovskiy's excellent [Wuff](https://github.com/akhikhl/wuff) and [Unpuzzle](https://github.com/akhikhl/unpuzzle) libraries have been a huge boon to everyone trying to get Gradle and Eclipse to collaborate.
* Thanks to Peter Kriens for the excellent [bnd](https://github.com/bndtools/bnd).
* Thanks to JRuyi and Agemo Cui for [osgibnd-gradle-plugin](https://github.com/jruyi/osgibnd-gradle-plugin), which inspired `BndManifestPlugin`.
* Thanks to [Scott Resnik](https://github.com/scottresnik) for [installed jre functionality](https://github.com/diffplug/goomph/pull/16), [line number and whitespace configuration](https://github.com/diffplug/goomph/pull/20), and [jdt config enhancements](https://github.com/diffplug/goomph/pull/23).
* Thanks to Stefan Oehme for his feedback on Goomph's design.
* Thanks to Neil Fraser of Google for [diff-match-patch](https://code.google.com/p/google-diff-match-patch/) which is very helpful for testing.
* Thanks to Thipor Kong for his [handy windows cmdline length workaround for the classpath](https://discuss.gradle.org/t/javaexec-fails-for-long-classpaths-on-windows/15266).
* Formatted by [spotless](https://github.com/diffplug/spotless).
* Bugs found by [findbugs](https://findbugs.sourceforge.net/).
* Built by [gradle](https://gradle.org/).
* Tested by [junit](https://junit.org/).
* Maintained by [DiffPlug](https://www.diffplug.com/).
