# <img align="left" src="images/goomph_logo.png"> Goomph: IDE as build artifact

<!---freshmark shields
output = [
	link(shield('Maven artifact', 'mavenCentral', '{{group}}:{{name}}', 'blue'), 'https://bintray.com/{{org}}/opensource/{{name}}/view'),
	link(shield('Latest version', 'latest', '{{stable}}', 'blue'), 'https://github.com/{{org}}/{{name}}/releases/latest'),
	link(shield('Javadoc', 'javadoc', 'OK', 'blue'), 'https://{{org}}.github.io/{{name}}/javadoc/{{stable}}/'),
	link(shield('License Apache', 'license', 'Apache', 'blue'), 'https://tldrlegal.com/license/apache-license-2.0-(apache-2.0)'),
	'',
	link(shield('Changelog', 'changelog', '{{version}}', 'brightgreen'), 'CHANGES.md'),
	link(image('Travis CI', 'https://travis-ci.org/{{org}}/{{name}}.svg?branch=master'), 'https://travis-ci.org/{{org}}/{{name}}'),
	link(shield('Live chat', 'gitter', 'live chat', 'brightgreen'), 'https://gitter.im/{{org}}/{{name}}')
	].join('\n');
-->
[![Maven artifact](https://img.shields.io/badge/mavenCentral-com.diffplug.gradle%3Agoomph-blue.svg)](https://bintray.com/diffplug/opensource/goomph/view)
[![Latest version](https://img.shields.io/badge/latest-3.5.0-blue.svg)](https://github.com/diffplug/goomph/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/goomph/javadoc/3.5.0/)
[![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-3.6.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/goomph.svg?branch=master)](https://travis-ci.org/diffplug/goomph)
[![Live chat](https://img.shields.io/badge/gitter-live_chat-brightgreen.svg)](https://gitter.im/diffplug/goomph)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
-->

## IDE-as-build-artifact.

It is possible to have many installations of the Eclipse IDE share a common set of installed artifacts, called a "bundlepool".  This means it is fast and efficient to get a purpose-built IDE for every project, preconfigured with all the plugins and settings appropriate for the project at hand.

When you run `gradlew ide`, it builds and downloads an IDE into `build/oomphIde` with just the features you need.  Takes ~15 seconds and 1MB of disk space once all the common artifacts have been cached at `~/.goomph`.

```groovy
apply plugin: 'com.diffplug.gradle.oomph.ide'
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

See the [plugin's javadoc](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/oomph/OomphIdePlugin.html) for a quickstart, and [HOW_TO_AUTOMATE_IDE.md](HOW_TO_AUTOMATE_IDE.md) for examples and more in-depth details.

## Blog posts

- [P2, Maven, and Gradle](http://discuss.diffplug.com/t/p2-maven-and-gradle)
- [Parting out eclipse](http://discuss.diffplug.com/t/parting-out-eclipse)

## Building OSGi bundles, Eclipse plugins, and RCP applications.

It turns out that the tooling required to implement "IDE-as-build-artifact" is the same tooling required to build Eclipse plugins and RCP applications in the first place.  That is Goomph's other side.  For a canonical example which demonstrates Goomph in use on a real project, see the [Gradle and Eclipse RCP talk](https://github.com/diffplug/gradle_and_eclipse_rcp).

Real world Eclipse software built with Goomph:
- [DiffPlug](https://www.diffplug.com/)
- [Veriluma](http://veriluma.com/)
- (your project here)

Below is an index of Goomph's capabilities, along with links to the javadoc where you can find usage examples.

**`com.diffplug.gradle.osgi` Plugin for generating OSGi metadata.**

* [`bndmanifest`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/osgi/BndManifestPlugin.html) generates a manifest using purely bnd, and outputs it for IDE consumption.
* [`OsgiExecable`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/osgi/OsgiExecable.html) makes it easy to run a chunk of code within an OSGi container, and get the result from outside the container.

**`com.diffplug.gradle.eclipse` Plugins for creating and manipulating eclipse project files.**

* [`buildproperties`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/eclipse/BuildPropertiesPlugin.html) uses [`build.properties`](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm) to control a gradle build, and fixes eclipse project classpath to include binary assets specified in `build.properties`.
* [`excludebuildfolder`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/eclipse/ExcludeBuildFolderPlugin.html) excludes the gradle `build` folder from Eclipse's resource indexing.
* [`projectdeps`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/eclipse/ProjectDepsPlugin.html) fixes an intermittent problem where dependencies on other projects within the workspace aren't always resolved correctly within Eclipse.
* [`resourcefilters`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/eclipse/ResourceFiltersPlugin.html) adds resource filters to the eclipse project.

**`com.diffplug.gradle.p2` Tasks and plugins for manipulating p2 data.**

* [`asmaven`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/AsMavenPlugin.html) downloads dependencies from a p2 repository and makes them available in a local maven repository.
* [`P2Model`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/P2Model.html) models a set of p2 repositories and IUs, and provides convenience methods for running p2-director or the p2.mirror ant task against these.
* [`P2AntRunner`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/P2AntRunner.html) runs eclipse ant tasks.
* [`FeaturesAndBundlesPublisher`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/FeaturesAndBundlesPublisher.html) models the FeaturesAndBundlesPublisher eclipse application.
* [`Repo2Runnable`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/Repo2Runnable.html) models the Repo2Runnable eclipse application.

**`com.diffplug.gradle.pde` Tasks for running Eclipse PDE using a downloaded eclipse instance.**

* [`PdeBuildTask`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/pde/PdeBuildTask.html) runs PDE build to build an RCP product.
* [`PdeAntBuildTask`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/pde/PdeAntBuildTask.html) runs PDE on an ant file.

**`com.diffplug.gradle.swt` Plugins for working with SWT in Gradle.**

* [`nativedeps`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/swt/NativeDepsPlugin.html) adds the platform-specific SWT jars to the runtime classpath so that SWT code can run.

**`com.diffplug.gradle` Miscellaneous infrastructure.**

* [`CmdLineTask`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/CmdLineTask.html) runs a series of shell commands, possibly copying or moving files in the meantime.
* [`JavaExecable`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/JavaExecable.html) makes it easy to run a chunk of code in a separate JVM, and get the result back in this one.
* [`JavaExecWinFriendly`](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/JavaExecWinFriendly.html) overcomes limitations in Windows' commandline length and long classpaths.

**`com.diffplug.gradle.eclipserunner` Infrastructure for running headless eclipse applications.**

* Used to power the infrastructure above.

<!---freshmark /javadoc -->

## Acknowledgements

* Andrey Hihlovskiy's excellent [Wuff](https://github.com/akhikhl/wuff) and [Unpuzzle](https://github.com/akhikhl/unpuzzle) libraries have been a huge boon to everyone trying to get Gradle and Eclipse to collaborate.
* Thanks to Peter Kriens for the excellent [bnd](https://github.com/bndtools/bnd).
* Thanks to JRuyi and Agemo Cui for [osgibnd-gradle-plugin](https://github.com/jruyi/osgibnd-gradle-plugin), which inspired `BndManifestPlugin`.
* Thanks to [Scott Resnik](https://github.com/scottresnik) for [installed jre functionality](https://github.com/diffplug/goomph/pull/16).
* Thanks to Stefan Oehme for his feedback on Goomph's design.
* Thanks to Neil Fraser of Google for [diff-match-patch](https://code.google.com/p/google-diff-match-patch/) which is very helpful for testing.
* Thanks to Thipor Kong for his [handy windows cmdline length workaround for the classpath](https://discuss.gradle.org/t/javaexec-fails-for-long-classpaths-on-windows/15266).
* Formatted by [spotless](https://github.com/diffplug/spotless).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
