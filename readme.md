## vgo

[![Build Status](https://github.com/jzbrooks/vgo/actions/workflows/build.yml/badge.svg?event=push)](https://github.com/jzbrooks/vgo/actions/workflows/build.yml)
[![Maven Central: vgo](https://img.shields.io/maven-central/v/com.jzbrooks/vgo?label=vgo)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo)
[![Maven Central: vgo-core](https://img.shields.io/maven-central/v/com.jzbrooks/vgo-core?label=vgo-core)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo-core)
[![Maven Central: vgo-plugin](https://img.shields.io/maven-central/v/com.jzbrooks/vgo-plugin?label=vgo-plugin)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo-plugin)

vgo optimizes vector graphics through a format-agnostic approach by leveraging vgo-core's intermediate representation. It not only optimizes the graphic but also converts between formats like SVG and Android Vector Drawables—with potential to support more formats in the future.

## Installation

#### Homebrew
`brew install jzbrooks/repo/vgo`

#### Manually
Download the distribution from the releases page and ensure it has execute permission. On macOS & Linux run `chmod u+x vgo`.

vgo requires Java 17.

## Gradle Plugin
The plugin aims to be fast and small by leveraging the JVM your gradle build is already using-no node-based tools are incorporated into your build.

The `shrinkVectorArtwork` task is added to your project on plugin application.

To incorporate the plugin in your build, configure maven central plugin resolution:
```groovy
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then, in the relevant project, add the plugin.

> [!NOTE]
> You must have the android tools sdk on your build classpath if you are converting SVGs to vector drawables. 
> This is typically done by applying the Android Gradle Plugin.

```groovy
plugins {
    id 'com.jzbrooks.vgo'
}

// Default configuration shown
vgo {
    inputs = fileTree(projectDir) {
        include '**/res/drawable*/*.xml'
    }
    outputs = inputs
    showStatistics = true
    format = OutputFormat.UNCHANGED
    noOptimization = false
    indent = 0
}
```

> [!TIP]
> For Android projects a non-zero indent is better for readability and provides no apk size impact after AAPT processing.

## Command Line Interface

```
> vgo [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent [value]  write files with value columns of indentation
  --format [value]  output format (svg, vd, etc) - ALPHA
  --no-optimiation  skip graphic optimization
```

> `java -jar vgo` for Windows

## Examples

```
# Optimize files specified from standard in
> find ./**/ic_*.xml | vgo

# Optimize vector.xml and overwrite its contents
> vgo vector.xml

# Optimize vector.xml and write the result into new_vector.xml
> vgo vector.xml -o new_vector.xml

# Optimize multiple input sources write results to the
> vgo vector.xml -o new_vector.xml ./assets -o ./new_assets
```

## Build instructions

This project uses the Gradle build system.

To build the binary: `./gradlew binary`

To run the tests: `./gradlew check`

To see all available tasks: `./gradlew tasks`
