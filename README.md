# <img align="left" src="images/goomph_logo.png"> Goomph: OSGi, Eclipse, and SWT for Gradle

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
[![Latest version](https://img.shields.io/badge/latest-2.1.1-blue.svg)](https://github.com/diffplug/goomph/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/goomph/javadoc/2.1.1/)
[![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-3.0.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/goomph.svg?branch=master)](https://travis-ci.org/diffplug/goomph)
[![Live chat](https://img.shields.io/badge/gitter-live_chat-brightgreen.svg)](https://gitter.im/diffplug/goomph)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
-->

Goomph has goals in two directions, whose implementations turn out to be tightly coupled:
1. Build OSGi bundles, Eclipse plugins, RCP applications, and P2 repositories using Gradle.  e.g. `gradlew buildRCP`
2. Make it easy for a Gradle project to provision an Eclipse IDE, workspace, and projects for the developer.  e.g. `gradlew ideSetup`.

Notable products using Goomph:
* [DiffPlug](http://www.diffplug.com/)
* (Your project here)

Below is an index of Goomph's capabilities, followed by more-detailed sections which describe their usage.  Of course, you can always consult the javadoc as described above.

* `com.diffplug.gradle.osgi`
	+ [`bndmanifest`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/osgi/BndManifestPlugin.html) generates a manifest using purely bnd, and outputs it for IDE consumption.
* `com.diffplug.gradle.eclipse`
	+ [`buildproperties`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/eclipse/BuildPropertiesPlugin.html) uses [`build.properties`](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm) to control a gradle build, and fixes eclipse project classpath to include binary assets specified in `build.properties`.
	+ [`excludebuildfolder`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/eclipse/ExcludeBuildFolderPlugin.html) excludes the gradle `build` folder from Eclipse's resource indexing.
	+ [`projectdeps`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/eclipse/ProjectDepsPlugin.html) fixes an intermittent problem where dependencies on other projects within the workspace aren't always resolved correctly within Eclipse.
* `com.diffplug.gradle.swt`
	+ [`nativedeps`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/swt/NativeDepsPlugin.html) adds the platform-specific SWT jars to the runtime classpath so that SWT code can run.
* `com.diffplug.gradle.pde`
	+ [`PdeProductBuildTask`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/pde/PdeProductBuildTask.html) runs PDE build to build an RCP product.
	+ [`PdeAntBuildTask`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/pde/PdeAntBuildTask.html) runs PDE on an ant file
	+ [`P2DirectorModel`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/pde/P2DirectorModel.html) runs P2 director to execute P2 actions.
	+ [`EclipsecTask`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/pde/EclipsecTask.html) runs a console command using `eclipsec`.
* `com.diffplug.gradle` (miscellaneous infrastructure)
	+ [`CmdLineTask`](https://diffplug.github.io/goomph/javadoc/2.1.1/com/diffplug/gradle/CmdLineTask.html) runs a series of shell commands, possibly copying or moving files in the meantime

<!---freshmark /javadoc -->

## Acknowledgements

* Andrey Hihlovskiy's excellent [Wuff](https://github.com/akhikhl/wuff) and [Unpuzzle](https://github.com/akhikhl/unpuzzle) libraries have been a huge boon to everyone trying to get Gradle and Eclipse to collaborate.
* Thanks to Peter Kriens for the excellent [bnd](https://github.com/bndtools/bnd).
* Thanks to JRuyi and Agemo Cui for [osgibnd-gradle-plugin](https://github.com/jruyi/osgibnd-gradle-plugin), which inspired `BndManifestPlugin`.
* Thanks to Neil Fraser of Google for [diff-match-patch](https://code.google.com/p/google-diff-match-patch/) which is very helpful for testing.
* Formatted by [spotless](https://github.com/diffplug/spotless), [as such](https://github.com/diffplug/durian/blob/v2.0/build.gradle?ts=4#L70-L90).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/), [as such](https://github.com/diffplug/durian/blob/v2.0/build.gradle?ts=4#L92-L116).
* Scripts in the `.ci` folder are inspired by [Ben Limmer's work](http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
