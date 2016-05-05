# <img align="left" src="images/goomph_logo.png"> Goomph: Automate Eclipse from Gradle

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
[![Latest version](https://img.shields.io/badge/latest-1.3.1-blue.svg)](https://github.com/diffplug/goomph/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/goomph/javadoc/1.3.1/)
[![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-2.0.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
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
	+ [`bndmanifest`](https://diffplug.github.io/goomph/javadoc/1.3.1/com/diffplug/gradle/osgi/BndManifestPlugin.html) generates a manifest using purely bnd, and outputs it for IDE consumption.
* `com.diffplug.gradle.eclipse`
	+ [`buildproperties`](https://diffplug.github.io/goomph/javadoc/1.3.1/com/diffplug/gradle/eclipse/BuildPropertiesPlugin.html) uses [`build.properties`](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm) to control a gradle build, and fixes eclipse project classpath to include binary assets specified in `build.properties`.
	+ `excludebuildfolder` excludes the gradle `build` folder from Eclipse's resource indexing.
	+ `projectdeps` fixes an intermittent problem where dependencies on other projects within the workspace aren't always resolved correctly within Eclipse.
* `com.diffplug.gradle.swt`
	+ `swt` adds the platform-specific SWT jars to the runtime classpath so that SWT code can run.
* `com.diffplug.gradle.pde`
	+ `PdeProductBuildTask` runs PDE build to build an RCP product.
	+ `P2DirectorModel` runs P2 director to execute P2 actions.
* `com.diffplug.gradle` (miscellaneous infrastructure)
	+ `CmdLineTask` runs a series of shell commands, possibly copying or moving files in the meantime

### `com.diffplug.gradle.osgi.bndmanifest`
Generating manifests by hand is a recipe for mistakes.  [Bnd](https://github.com/bndtools/bnd) does a fantastic job generating all this stuff for you, but there's a lot of wiring required to tie it into both Eclipse PDE and Gradle.  Which is what Goomph is for!

```groovy
apply plugin: 'com.diffplug.gradle.eclipse.bndmanifest'
// pass headers and bnd directives: http://www.aqute.biz/Bnd/Format
jar.manifest.attributes(
	'-exportcontents': 'com.diffplug.*',
	'-removeheaders': 'Bnd-LastModified,Bundle-Name,Created-By,Tool,Private-Package',
	'Import-Package': '!javax.annotation.*,*',
	'Bundle-SymbolicName': "${project.name};singleton:=true",
)
```

Besides passing raw headers and bnd directives, this plugin takes the following actions:
- Passes the project version to bnd.
- Passes the `runtime` coonfiguration's classpath to bnd for manifest calculation.
- Instructs bnd to respect the result of the `processResources` task.
- Writes out the resultant manifest to `META-INF/MANIFEST.MF`, so that your IDE stays up-to-date.

### `com.diffplug.gradle.eclipse.buildproperties`
Eclipse PDE uses a [`build.properties`](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.pde.doc.user%2Fguide%2Ftools%2Feditors%2Fmanifest_editor%2Fbuild.htm) file to control the build process.  Even if you aren't using PDE to do your build, the IDE will throw warnings if you don't keep the build.properties up to date.

This plugin reads the `build.properties` file, and uses that to setup the Gradle `processResources` task.  It also ensures that these resources are available on the IDE's classpath.  This way your `build.properties` can be the single source of truth for all the binary assets inside your plugin.

```groovy
apply plugin: 'com.diffplug.gradle.eclipse.buildproperties'
```

### `com.diffplug.gradle.eclipse.excludebuildfolder`
If you hit `Ctrl + R` in eclipse, you'll get a fuzzy search for resources in your workspace.  This will include class files and other artifacts in the Gradle build folder, which is usually not desirable.  To fix:

```groovy
apply plugin: 'com.diffplug.gradle.eclipse.excludebuildfolder'
```

### `com.diffplug.gradle.eclipse.projectdeps`
Fixes a problem where dependencies on other projects within the workspace aren't always resolved correctly within Eclipse.

```groovy
apply plugin: 'com.diffplug.gradle.eclipse.projectdeps'
```

### `com.diffplug.gradle.swt.nativedeps`
Adds the platform-specific SWT jars wich are appropriate for the native system as `compile` dependencies.

```groovy
apply plugin: `com.diffplug.gradle.swt`
// (configuration block below is optional)
goomphSwtNativeDeps {
	version = '4.5.2' // currently supported: 4.4.2, 4.5.2, defaults to the latest available
}
```

### `com.diffplug.gradle.pde.PdeProductBuildTask`

*requires wuff

Runs PDE build to make an RCP application or a P2 repository.

```groovy
import com.diffplug.gradle.*
import com.diffplug.gradle.pde.*
import com.diffplug.common.swt.os.*

task diffplugP2(type: PdeProductBuildTask) {
	// the directory which will contain the results of the build
	buildDir(P2_BUILD_DIR)
	// copy the product file and its referenced images
	copyProductAndImgs('../com.diffplug.core', 'plugins/com.diffplug.core')
	// set the plugins to be the delta pack (implicit)
	// and the combined targetplatform / obfuscation result
	setPluginPath(PDE_PREP_DIR)
	// if multiple versions of a plugin are detected between the pluginPath / targetplatform,
	// you must list the plugin name, and all versions which are available.  only the first
	// plugin will be included in the final product build
	resolveWithFirst('org.apache.commons.codec', '1.9.0', '1.6.0.v201305230611')
	resolveWithFirst('org.apache.commons.logging', '1.2.0', '1.1.1.v201101211721')
	// set the build properties to be as shown
	addBuildProperty('topLevelElementType',	'product')
	addBuildProperty('topLevelElementId',	APP_ID)
	addBuildProperty('product', '/com.diffplug.core/' + APP_ID)
	addBuildProperty('runPackager', 'false')
	addBuildProperty('groupConfigurations',		'true')
	addBuildProperty('filteredDependencyCheck',	'true')
	addBuildProperty('resolution.devMode',		'true')
	// configure some P2 pieces
	addBuildProperty('p2.build.repo',	'file:' + project.file(P2_REPO_DIR).absolutePath)
	addBuildProperty('p2.gathering',	'true')
	addBuildProperty('skipDirector',	'true')

	// configure gradle's staleness detector
	inputs.dir(PDE_PREP_DIR)
	outputs.dir(P2_REPO_DIR)

	doLast {
		// artifact compression reduces content.xml from ~1MB to ~100kB
		def compressXml = { name ->
			def xml = project.file(P2_REPO_DIR + "/${name}.xml")
			def jar = project.file(P2_REPO_DIR + "/${name}.jar")
			ZipUtil.zip(xml, "${name}.xml", jar)
			xml.delete()
		}
		compressXml('artifacts')
		compressXml('content')
	}
}

```

### `com.diffplug.gradle.pde.P2DirectorModel`

*requires wuff

Runs P2 director to install artifacts from P2 repositories.

```groovy
import com.diffplug.gradle.*
import com.diffplug.gradle.pde.*
import com.diffplug.common.swt.os.*

// list of OS values for which we want to create an installer
def INSTALLERS = OS.values()
def VER_JRE = '1.8.0.40'

// add each of the core IUs
def coreModel = new P2DirectorModel()
coreModel.addIU(APP_ID)
coreModel.addIU('com.diffplug.jre.feature.group')
coreModel.addIU('com.diffplug.native.feature.group')

// add each of the local repositories
def repoRoot = 'file:' + projectDir + '/'	// reads repos from this machine
//def repoRoot = 'http://192.168.1.77/'		// reads repos from another machine running hostFiles()
def assembleModel = coreModel.copy()
assembleModel.addRepo(repoRoot + P2_REPO_DIR)
ROOT_FEATURES.forEach() { feature ->
	assembleModel.addRepo('file:' + project.file(ROOT_FEATURE_DIR + feature))
}

// assemble DiffPlug for each os
task assembleAll
def ASSEMBLE_TASK = 'assemble'
def assembleDir(OS os) { return project.file('build/10_assemble' + os + (os.isMac() ? ".app" : "")) }
INSTALLERS.each() { os ->
	def assembleOneTask = assembleModel.taskFor(project, ASSEMBLE_TASK + os, os, assembleDir(os))
	assembleOneTask.dependsOn(diffplugP2)
	assembleOneTask.dependsOn(checkRootFeatures)
	assembleAll.dependsOn(assembleOneTask)

	// make the JRE executable if we can
	if (os.isMacOrLinux() && NATIVE_OS.isMacOrLinux()) {
		EclipsePlatform platform = EclipsePlatform.fromOS(os)
		assembleOneTask.doLast {
			def JRE_DIR = project.file(assembleDir(os).absolutePath + '/features/com.diffplug.jre.' + platform + '_' + VER_JRE + '/jre')
			CmdLine.runCmd(JRE_DIR, 'chmod -R a+x .')
		}
	}
}

// test case which creates a DiffPlug from an existing DiffPlug
task reassembleAll
def REASSEMBLE_TASK = 'reassemble'
def reassembleDir(OS os) { return project.file('build/11_reassemble' + os + (os.isMac() ? ".app" : "")) }
INSTALLERS.each() { os ->
	def reassembleModel = coreModel.copy()
	def reassembleRoot = 'file:' + assembleDir(os)
	reassembleModel.addMetadataRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.engine/profileRegistry/ProfileDiffPlugP2.profile')
	reassembleModel.addArtifactRepo(reassembleRoot + '/p2/org.eclipse.equinox.p2.core/cache')
	reassembleModel.addArtifactRepo(reassembleRoot)
	def reassembleOneTask = reassembleModel.taskFor(project, REASSEMBLE_TASK + os, os, reassembleDir(os))
	reassembleOneTask.dependsOn(ASSEMBLE_TASK + os)
	reassembleAll.dependsOn(reassembleOneTask)
}
```

### *requires wuff
These parts of Goomph currently have the following precondition: your project must have the property `VER_ECLIPSE=4.5.2` (or some other number), and you must have installed that Eclipse using [Wuff](https://github.com/akhikhl/wuff).  In the future, we hope to use [Oomph](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.oomph.targlets.doc%2Fjavadoc%2Findex.html&help-doc.html) to procure an Eclipse installation, but for now these pieces assumes that you're building your plugins using Wuff, and Goomph is for bundling all that stuff together.

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
