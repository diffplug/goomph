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
[![Latest version](https://img.shields.io/badge/latest-1.2.0-blue.svg)](https://github.com/diffplug/goomph/releases/latest)
[![Javadoc](https://img.shields.io/badge/javadoc-OK-blue.svg)](https://diffplug.github.io/goomph/javadoc/1.2.0/)
[![License Apache](https://img.shields.io/badge/license-Apache-blue.svg)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))

[![Changelog](https://img.shields.io/badge/changelog-1.3.0--SNAPSHOT-brightgreen.svg)](CHANGES.md)
[![Travis CI](https://travis-ci.org/diffplug/goomph.svg?branch=master)](https://travis-ci.org/diffplug/goomph)
[![Live chat](https://img.shields.io/badge/gitter-live_chat-brightgreen.svg)](https://gitter.im/diffplug/goomph)
<!---freshmark /shields -->

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
-->

This project has goals in two directions, whose implementations turn out to be tightly coupled:

1. Build Eclipse plugins, RCP applications, and P2 repositories using Gradle.  e.g. `gradlew buildRCP`
2. Make it easy for a Gradle project to provision an Eclipse IDE and workspace for the developer.  e.g. `gradlew ideSetup`.

Goal 1 is fully but hastily implemented.  Goal 2 is almost entirely unimplemented.  This project is used to build and ship [DiffPlug](http://www.diffplug.com/), which involves:

- building an RCP application
- building an RCP installer application
- building a P2 repo for updating
- downloading from that P2 site to test that updates will work

Goomph isn't a Gradle plugin, per say.  It provides a few Gradle tasks, which call out to an Eclipse installation to do things.  It can also be used simply as a Java library.

Goomph currently has the following precondition: your project must have the property `VER_ECLIPSE=4.4` (or some other number), and you must have installed that Eclipse using [Wuff](https://github.com/akhikhl/wuff).  In the future, we hope to use [Oomph](http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.oomph.targlets.doc%2Fjavadoc%2Findex.html&help-doc.html) to procure an Eclipse installation, but for now the project assumes that you're building your plugins using Wuff, and Goomph is for bundling all that stuff together.

This is mostly a "huck it over the wall" kind of open-sourcing, along with a gentle prod to the community that Gradle and Oomph have a huge amount to gain from each other.  Hopefully later on I'll be able to devote some real resources to making this happen.

Contributions are welcome, see [the contributing guide](CONTRIBUTING.md) for development info.  Copy-pasting from this project into a totally new or very similar project is also welcome!

Here are the things it can do as of now:

### run PDE build (makes an RCP application or a P2 repository)

```groovy
import com.diffplug.gradle.*
import com.diffplug.gradle.eclipse.*
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

### run P2 director (assemble an RCP application from a P2 repository)

```groovy
import com.diffplug.gradle.*
import com.diffplug.gradle.eclipse.*
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

<!---freshmark /javadoc -->

## Acknowledgements

* Andrey Hihlovskiy's excellent [Wuff](https://github.com/akhikhl/wuff) and [Unpuzzle](https://github.com/akhikhl/unpuzzle) libraries have been a huge boon to everyone trying to get Gradle and Eclipse to collaborate.
* Formatted by [spotless](https://github.com/diffplug/spotless), [as such](https://github.com/diffplug/durian/blob/v2.0/build.gradle?ts=4#L70-L90).
* Bugs found by [findbugs](http://findbugs.sourceforge.net/), [as such](https://github.com/diffplug/durian/blob/v2.0/build.gradle?ts=4#L92-L116).
* Scripts in the `.ci` folder are inspired by [Ben Limmer's work](http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/).
* Built by [gradle](http://gradle.org/).
* Tested by [junit](http://junit.org/).
* Maintained by [DiffPlug](http://www.diffplug.com/).
