<img width="256" alt="vgo" src="https://github.com/user-attachments/assets/96047506-42f5-48cd-8ed1-fc87906fc621" style="object-align:center"/>

[![Build Status](https://github.com/jzbrooks/vgo/actions/workflows/build.yml/badge.svg?event=push)](https://github.com/jzbrooks/vgo/actions/workflows/build.yml)
[![Maven Central: vgo](https://img.shields.io/maven-central/v/com.jzbrooks/vgo?label=vgo)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo)
[![Maven Central: vgo-core](https://img.shields.io/maven-central/v/com.jzbrooks/vgo-core?label=vgo-core)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo-core)
[![Maven Central: vgo-plugin](https://img.shields.io/maven-central/v/com.jzbrooks/vgo-plugin?label=vgo-plugin)](https://ossindex.sonatype.org/component/pkg:maven/com.jzbrooks/vgo-plugin)

vgo optimizes vector graphics through a format-agnostic approach by leveraging vgo-core's intermediate representation.
It can convert between common vector formats, including SVG, Android Vector Drawables, and Jetpack Compose ImageVector _and_ optimize them to boot.

## Installation

#### Homebrew
`brew install jzbrooks/repo/vgo`

#### Manually
Download the distribution from the release page and ensure it has execute permission. On macOS & Linux run `chmod u+x vgo`.

vgo requires Java 17.

## Gradle Plugin
The plugin aims to be fast and small by leveraging (for the entire tool) the JVM your Gradle build is already using.

The `shrinkVectorGraphic` task is added to your project on plugin application.

The `checkVectorGraphic` task is also added, a verification counterpart that fails the build if any
vector graphic is not fully shrunk (without modifying any files)

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
> You must have the kotlin compiler on your build classpath if you are using the `ImageVector` format.
> This is typically done by applying the Kotlin Gradle Plugin.

```groovy
plugins {
    id 'com.jzbrooks.vgo'
}

// Default configuration shown
vgo {
    inputs = fileTree(projectDir) {
        include '**/res/drawable*/*.xml'
        exclude '**/build/**'
    }
    outputs = inputs // omit to optimize files in place
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
  -h --help          print this message
  -o --output        file or directory, if not provided the input will be overwritten
  -s --stats         print statistics on processed files to standard out
  -v --version       print the version number
  --indent [value]   write files with value columns of indentation
  --format [value]   output format (svg, vd, iv)
  --no-optimization  skip graphic optimization
  --stdin            read newline-delimited file paths from standard input
  --check            verify inputs are fully shrunk without writing; prints files that would change and exits non-zero
  --print-ir[=MODE]  print IR tree and exit without writing (auto [default], color, plain; use = to pass mode)  
```

> `java -jar vgo` for Windows

## vgo vs. SVGO benchmark

Generated: 2026-08-08T01:08:39.241Z

Corpus: `/Users/justin/projects/vgo/vgo/src/test/resources` (10 files, 16733816 bytes)

Runs: 10; warmups: 3; batch size: 100

| Tool | Version | Mean ± σ | Median | Min | Max | Output bytes | Saved | MiB/s | Relative |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| vgo | 5.0.0 | 3.266 s ± 0.035 s | 3.265 s | 3.226 s | 3.343 s | 11983741 | 28.39% | 4.89 | 1.00× |
| svgo | 4.0.2 | 9.499 s ± 0.044 s | 9.494 s | 9.417 s | 9.574 s | 11123262 | 33.53% | 1.68 | 2.91× |

| File | Original | vgo | SVGO | Smaller output |
|---|---:|---:|---:|---|
| android.svg | 822 | 716 | 679 | svgo |
| dribbble_ball_mark.svg | 1874 | 1279 | 1207 | svgo |
| eleven_below_single.svg | 141009 | 79899 | 62480 | svgo |
| gradient_linear.svg | 700 | 505 | 562 | vgo |
| great_wave.svg | 16497061 | 11814855 | 10973810 | svgo |
| guacamole.svg | 4012 | 2845 | 2776 | svgo |
| nasa.svg | 6592 | 6592 | 5667 | svgo |
| simple_heart.svg | 244 | 177 | 169 | svgo |
| tiger.svg | 68630 | 68630 | 68451 | svgo |
| vgo.svg | 12872 | 8243 | 7461 | svgo |

> Timing includes CLI startup, parsing, optimization, and writes. Corpus setup and validation are excluded. Structural validation does not prove visual equivalence.

## Examples

### CLI
```
# Optimize files specified from standard in
> find ./**/ic_*.xml | vgo --stdin

# Optimize vector.xml and overwrite its contents
> vgo vector.xml

# Optimize vector.xml and write the result into new_vector.xml
> vgo vector.xml -o new_vector.xml

# Optimize multiple input sources write results to the
> vgo vector.xml -o new_vector.xml ./assets -o ./new_assets

# Fail (non-zero exit) if any vector is not fully shrunk — useful in CI
> vgo --check ./assets
```

### Gradle Plugin
```kotlin
// Optimize and convert svgs to vector drawables at build time
vgo {
    format = OutputFormat.VECTOR_DRAWABLE
    inputs = fileTree(projectDir) {
        include("icons/**/*.svg")
    }
    val drawableDir = layout.projectDirectory.dir("src/main/res/drawable")
    outputs.setFrom(
        inputs.elements.map { svgs ->
            svgs.map { svg -> drawableDir.file("${svg.asFile.nameWithoutExtension}.xml").asFile }
        }
    )
}

tasks.named("processDebugResources") {
    dependsOn("shrinkVectorGraphic")
}
```

## Build instructions

This project uses the Gradle build system.

To build the binary: `./gradlew binary`

To run the tests: `./gradlew check`

To see all available tasks: `./gradlew tasks`
