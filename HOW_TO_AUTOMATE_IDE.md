# How to automate IDE generation

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://{{org}}.github.io/{{name}}/javadoc/', '/', stable);
-->

So you want to automate your IDE configuration.  The first thing to do is look at a few examples.

- [Gradle and Eclipse RCP talk](https://github.com/diffplug/gradle_and_eclipse_rcp/blob/master/ide/build.gradle) (multi-project Eclipse RCP project)
- [ls-api](https://github.com/TypeFox/ls-api/blob/61a3089569acbe159f043534f282401452a34bc3/ide/build.gradle) (xtend IDE example)
- [Spotless](https://github.com/diffplug/spotless/blob/master/build.gradle) (single-project Gradle plugin)
- (your example here)

The next thing is to look at the [javadoc](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/oomph/OomphIdePlugin.html) for `OomphIdePlugin`, which inclues a pretty in-depth look at how it works.

## How do I automate ${MY_THING} which isn't in the examples or docs?

First off, search the [issues on GitHub](https://github.com/diffplug/goomph/issues) to see if somebody already tried to automate your thing.  If they didn't make a new one to document what you find and maybe somebody can help you too!

Eclipse gets its behavior from three places:

1. project files (`.project`, `.classpath`, etc.) which live in the individual project folders
2. plugin and feature jars (installed by p2)
3. workspace settings files (`workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/something.prefs`)

Depending on what you're trying to automate, you might need to touch all three.

Manipulating project files is a [core part of gradle](https://docs.gradle.org/current/userguide/eclipse_plugin.html), so we won't cover that here.

Manipulating plugin and feature jars has lots of coverage in the examples above.  The entire `oomphIdeBlock` extends [P2Declarative](https://diffplug.github.io/goomph/javadoc/3.5.0/com/diffplug/gradle/p2/P2Declarative.html).  So if you want to add any features, all you need to do is make sure you add all the necessary p2 repositories, and then specify the features or installable units that you need.

Manipulating the workspace is where it gets tricky.  We'll dig in below:

## How do I find and set a workspace setting / preference?

The general formula for setting a particular setting is this:

- Create and open a clean IDE `gradlew ideClean ide`
- Close the IDE (which will save the workspace metadata)
- Find the workspace directory (it will be inside `~/.goomph/ide-workspaces`)
- Copy the workspace directory
- Open the IDE `gradlew ide` and set the settings you'd like to set
- Close the IDE (which will save the workspace metadata)
- Diff the workspace directory before and after

This will let you know which property file you need to set, and what you need to set it to.  Once you know what you need to do, this is how you can actually do it:

### Set a workspace property file

Most eclipse settings are set in property files.  You can set them manually like this:

```gradle
oomphIde {
	...
	workspaceProp '.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs', {
		it.put('themeid', 'org.eclipse.e4.ui.css.theme.e4_classic')
	}
}
```

### Run programmatically

You can also use Eclipse's internal APIs to programatically set properties.  This is pretty advanced.  You'll want to look at Goomph's code very carefully, and examine the subclasses of `SetupAction`.

##  Contribute back!

If you find out how to set a useful setting, please consider contributing it back!  Key places where it might make sense to contribute:

- [ConventionStyle](https://github.com/diffplug/goomph/blob/master/src/main/java/com/diffplug/gradle/oomph/ConventionStyle.java) is the `style{}` block in `oomphIde`.
- [ConventionJdt](https://github.com/diffplug/goomph/blob/master/src/main/java/com/diffplug/gradle/oomph/ConventionJdt.java) is the `jdt{}` block in `oomphIde`.
- [ConventionPde](https://github.com/diffplug/goomph/blob/master/src/main/java/com/diffplug/gradle/oomph/ConventionPde.java) is the `pde{}` block in `oomphIde`.
- or a new subclass of [OomphConvention](https://github.com/diffplug/goomph/blob/master/src/main/java/com/diffplug/gradle/oomph/OomphConvention.java) to create a new way of grouping settings.

<!---freshmark /javadoc -->
