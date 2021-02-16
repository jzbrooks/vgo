## vgo

vgo is a tool for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.

[![Build Status](https://github.com/jzbrooks/vgo/workflows/build/badge.svg)](https://github.com/jzbrooks/vgo/actions?workflow=build)

## Installation

Download the distribution from the releases page and ensure it has execute permission. On macOS & Linux run `chmod +x vgo`.

## Gradle Plugin
Add the plugin to your buildscript classpath.
```
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath "com.jzbrooks:vgo-plugin:1.4.0"
    }
}
```

Then, in the relevant project, add the plugin.
```
plugins {
    id 'com.jzbrooks.vgo'
}

vgo {
    inputs = fileTree(projectDir) {
        include '**/res/drawable*/*.xml'
    }
    outputs = inputs
    showStatistics = true
    format = OutputFormat.UNCHANGED
    indent = 0
}
```

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

To build the binary: `/.gradlew binary`

To run the tests: `./gradlew check`

To see all available tasks: `./gradlew tasks`
