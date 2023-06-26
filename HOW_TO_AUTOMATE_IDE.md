# How to automate IDE generation

<!---freshmark javadoc
output = prefixDelimiterReplace(input, 'https://javadoc.io/doc/com.diffplug.gradle/goomph/', '/', versionLast);
-->

So you want to automate your IDE configuration.  The first thing to do is look at a few examples.

- [Gradle and Eclipse RCP talk](https://github.com/diffplug/gradle_and_eclipse_rcp/blob/main/ide/build.gradle) (multi-project Eclipse RCP project)
- [lsp4j](https://github.com/eclipse/lsp4j/blob/main/ide/build.gradle) (xtend IDE example)
- [Spotless](https://github.com/diffplug/spotless/blob/gradle/5.17.1/ide/build.gradle) (single-project Gradle plugin)
- (your example here)

The next thing is to look at the [javadoc](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.42.1/com/diffplug/gradle/oomph/OomphIdePlugin.html) for `OomphIdePlugin`, which inclues a pretty in-depth look at how it works.

## How do I automate ${MY_THING} which isn't in the examples or docs?

First off, search the [issues on GitHub](https://github.com/diffplug/goomph/issues) to see if somebody already tried to automate your thing.  If they didn't make a new one to document what you find and maybe somebody can help you too!

Eclipse gets its behavior from three places:

1. project files (`.project`, `.classpath`, etc.) which live in the individual project folders
2. plugin and feature jars (installed by p2)
3. workspace settings files (`workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/something.prefs`)

Depending on what you're trying to automate, you might need to touch all three.

Manipulating project files is a [core part of gradle](https://docs.gradle.org/current/userguide/eclipse_plugin.html), so we won't cover that here.

Manipulating plugin and feature jars has lots of coverage in the examples above.  The entire `oomphIdeBlock` extends [P2Declarative](https://javadoc.io/doc/com.diffplug.gradle/goomph/3.42.1/com/diffplug/gradle/p2/P2Declarative.html).
So if you want to add any features, you just add the required p2 repositories, and then specify the features or installable units that you need.

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

### Set a workspace property or xml file

Most eclipse settings are set in property files.  You can set them manually like this:

```gradle
oomphIde {
  ...
  workspaceProp '.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.e4.ui.css.swt.theme.prefs', {
    it.put('themeid', 'org.eclipse.e4.ui.css.theme.e4_classic')
  }
}
```

You can also set xml files.  In order to set an xml file, you must first provide a template, and then you can modify the xml using [`XmlProvider`](https://docs.gradle.org/current/javadoc/org/gradle/api/XmlProvider.html).

```gradle
oomphIde {
  ...
  workspaceFile('destination', 'source')
  workspaceXml('destination', { xmlProvider -> ...}) // modify your xml here
}
```

### Modify a setting programmatically

You can also use Eclipse's internal APIs to programatically set properties.  This is pretty advanced.  You'll want to look at Goomph's code, and examine the subclasses of `SetupAction`.

## How do I add a DSL for a plugin?

If you have an Eclipse plugin that you'd like to add to to Goomph, we'd love to have it!  Take a look at [ConventionThirdParty](https://github.com/diffplug/goomph/blob/main/src/main/java/com/diffplug/gradle/oomph/thirdparty/ConventionThirdParty.java), and add a block for your plugin there.  If you look at all subclasses of `OomphConvention`, you can see how you can make a configuration DSL for your users, if you'd like.  But even just a simple "add the repo, add the feature" is helpful.

##  Contribute back!

If you find out how to set a useful setting, please consider contributing it back!  Key places where it might make sense to contribute:

- [ConventionStyle](https://github.com/diffplug/goomph/blob/main/src/main/java/com/diffplug/gradle/oomph/ConventionStyle.java) is the `style{}` block in `oomphIde`.
- [ConventionJdt](https://github.com/diffplug/goomph/blob/main/src/main/java/com/diffplug/gradle/oomph/ConventionJdt.java) is the `jdt{}` block in `oomphIde`.
- [ConventionPde](https://github.com/diffplug/goomph/blob/main/src/main/java/com/diffplug/gradle/oomph/ConventionPde.java) is the `pde{}` block in `oomphIde`.
- or a new subclass of [OomphConvention](https://github.com/diffplug/goomph/blob/main/src/main/java/com/diffplug/gradle/oomph/OomphConvention.java) to create a new way of grouping settings.

<!---freshmark /javadoc -->
