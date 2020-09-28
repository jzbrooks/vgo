Change Log
==========

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
