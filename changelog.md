Change Log
==========

## Unpublished (2.0.0)

* Removed non IR related code from the core library
* Fixed tests on Windows

## 1.4.1
_02.16.2021_

* Added: A new type for the `shrinkVectorArtwork` task

## 1.4.0
_02.15.2021_

* Added: Gradle plugin
* Improved: Reworked the gradle modules to better be published as a library (vgo-core) and thin application wrapper (vgo).
* Added: Sonatype publishing

## 1.3.0
_01.18.2021_

* Added: Collapse multiple Bézier curves into elliptical arcs when possible
* Improvement: Target JVM 11

## 1.2.2
_10.20.2020_

* Improvement: Show filenames with statistics with multiple file inputs and `--stats`
* Improvement: Remove Kotlin metadata from the output jar
* Fixed: Temporarily removed an optimization that distorted some images

## 1.2.1
_10.01.2020_

* Fixed: Some images with curves that lie on a circle omit any representation of that circle in the output
* Fixed: Modifying files in-place sometimes results in destroying non-vector files.

## 1.2.0
_09.28.2020_

* Improvement: Resort to distribution via a fat jar. Requires managing fewer files and results in a smaller installation since R8 can operate on classes from dependencies as well.
* Improvement: Use R8 for optimization. R8 produces a slightly smaller jar and in some cases faster code as well.

## 1.1.1
_07.13.2020_

* Fixed: A crash when running on a file in the current directory
* Improvement: Report an error when an input file doesn't exist

## 1.1.0
_06.20.2020_

* New: Remove redundant close path commands
* Improvement: Use the Gradle Application Plugin to build application distrobutions, simplifying installation and making running the tool a little simpler.
