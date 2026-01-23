# Changelog

## Unreleased

### Added

### Changed
- `com.jzbrooks.vgo.core.transformation.MergePaths` more precisely checks if two paths overlap, which encourages merging

### Deprecated

### Removed

### Fixed

### Security

## 3.2.1 - 2025-05-10

### Fixed

- Removed the kotlin compiler from the build classpath to avoid undefined behavior when the Gradle plugin is applied
- The `vgo` Gradle extension's `outputs` property are honored

## 3.2.0 - 2025-05-03

### Added

- An option to disable optimizations for conversion-only use cases (`--no-optimization` cli flag and `com.jzbrooks.vgo.plugin.VgoPluginExtension.noOptimization` for the gradle plugin)
- _Experimental_ support for `ImageVector` generation and optimization
  - _No Clip Paths_: Clip paths are not supported in ImageVector generation. They will be ignored. 
  - _Overwriting files is not recommended_: Since the internal representation of ImageVectors is incomplete, overwriting source files may result in data loss.
  - _Suboptimal thread safety_: The generated image vectors could be created multiple times during their initialization if the first two reads happen simultaneously.
  - _Specific parsing constraints_: The ImageVector parser only parses image vector properties of the form output by the vgo. A sketch of the shape of the output format:  
    ```kotlin
    val vector: ImageVector
        get() = _vector ?: ImageVector.Builder(/* ... */)
            .path(/* ... */) { /* ... */ }
            .group(/* ... */) { /* ... */ }
            // ...
            .build().also { _vector = it }

    private var _vector: ImageVector? = null
    ```
- `ExperimentalVgoApi` opt-in annotation for experimental portions of the API
- `com.jzbrooks.vgo.core.util.math.computeTransformation` for computing a transformation matrix from common transform parameters

## 3.1.0 - 2025-03-13

### Added

- `com.jzbrooks.vgo.util.parse` parses a file into support `Graphic` subtypes if possible

### Changed

- Relocated classes that transform vector IR from `com.jzbrooks.vgo.core.optimization` to `com.jzbrooks.vgo.core.transformation` and renamed some relevant classes. The former package is deprecated in favor of the latter, with a strong 1:1 correspondence between structures (modulo names).

### Fixed

- Incorrect parsing of elliptical arc parameters when no separator was present between flag parameters
- NPE when converting an SVG to Vector Drawable in some cases (#139)

## 3.0.0 - 2024-11-30

### Added

- `com.jzbrooks.vgo.core.util.math.Surveyor`, which computes the bounding box of an arbitrary list of commands 
- Bézier curve interpolation for all variants and elliptical arc bounding box functions

### Changed

- `vgo-plugin` (`com.jzbrooks.vgo.plugin`) no longer requires a particular version of Android Gradle Plugin.
  Note: `:vgo` is an abstract implementation of the tool which does not assume either a cli or plugin context. CLI related logic has been relocated into `:vgo-cli`.
- **Breaking:** `CubicCurve<*>.interpolate` has been split into `CubicBezierCurve.interpolate` and `SmoothCubicBezierCurve.interpolate`
- `com.jzbrooks.vgo.core.optimization.MergePaths` constructor accepts constraints. See `com.jzbrooks.vgo.core.optimization.MergePaths.Constraints`.
- Paths with an even odd fill rule can be merged

### Fixed

- Overlapping paths are no longer merged, which avoids some image warping issues (#88, #101)
- Conversions without a specified output file will write a file the file extension corresponding to the format.
- Decimal separators are locale-invariant. (#60)
- Crash when using the CLI to convert an SVG containing a clip path to vector drawable.
- (Vector Drawable) Path merging avoids merging a single path data string beyond the framework string length limit (#82)
- Paths with an initial relative command are modified to make that command absolute when merged (#111)

## 2.4.0 - 2024-10-02

### Changed

- Support AGP 8.7.0. This is a stop-gap solution which will be improved in #89.

## 2.3.0 - 2024-09-21

### Added

- Validate binary compatibility of vgo-core's API

### Changed

- Rolled back the JVM requirement to 17

## 2.2.3 - 2024-09-16

### Fixed

- A bug that would sometimes introduce a grouping comma into a large number, breaking path data. This most often occurred in the convert curves to arcs optimization when an arc with a large radius was more compact than the corresponding Bézier curve. 
- Thread safety issues that prevented the `shrinkVectorArtwork` task from being executed in parallel in a highly-modularized parallel Gradle build.

## 2.2.2 - 2024-08-30

### Fixed

- A crash when running `shrinkVectorArtwork` in a highly-modularized parallel Gradle build

## 2.2.1 - 2024-08-21

### Fixed

- Gradle plugin hangs for projects with no vector drawables

## 2.2.0 - 2024-08-13

### Added

- More robust SVG → vector conversions by Android Studio tools (#47)
- More compact printing of 2D coordinates when y < 0 for vector drawables

### Fixed

- In rare cases, subpath start points were tracked incorrectly which resulted in a crash (#57)
- Disabled `ConvertCurvesToArcs` until some edge cases can be worked out (#65)

## 2.1.0 - 09-14-2021

- New: Simplified optimization machinery with `ElementVisitor`
- Improvement: `MergePaths` no longer requires its own tree traversal
- Improvement: Attribute values omit leading zeros where possible
- Fix: theme referenced colors like `?attrs/dark` no longer cause crashes
- Upgrade: Build tools

## 2.0.2 - 06.02.2021

- Fixed: Vector Drawable shorthand hex colors (like #FFF) are properly handled

## 2.0.1 - 06.01.2021

- Fixed: Groups that wrap clip paths are no longer removed

## 2.0.0 - 05.29.2021

- New, breaking(vgo-core): Structured graphic element attributes. 
  * This greatly simplifies the conversions between formats
  * Unstructured attributes will be dropped during conversion between formats
- Improvement, breaking(vgo): Unified the clip path implementation
- Improvement, breaking(vgo): Format-specific implementations of optimizations have been traded for format independent implementations, so they can be more easily used by new formats
- Improvement: The new implementation of the bake transformations optimization operates in many more situations for svg compared to the previous implementation
- Improvement: Remove redundant commands in situations where the command is effectively but not precisely redundant
- Fixed: Redundant move commands are removed
- Fixed: Test failures on Windows due to path handling
- Upgrade: Build with Kotlin 1.5.10

## 1.4.1 - 02.16.2021

- Added: A new type for the `shrinkVectorArtwork` task

## 1.4.0 - 02.15.2021

- Added: Gradle plugin
- Improved: Reworked the gradle modules to better be published as a library (vgo-core) and thin application wrapper (vgo).
- Added: Sonatype publishing

## 1.3.0 - 01.18.2021

- Added: Collapse multiple Bézier curves into elliptical arcs when possible
- Improvement: Target JVM 11

## 1.2.2 - 10.20.2020

- Improvement: Show filenames with statistics with multiple file inputs and `--stats`
- Improvement: Remove Kotlin metadata from the output jar
- Fixed: Temporarily removed an optimization that distorted some images

## 1.2.1 - 10.01.2020

- Fixed: Some images with curves that lie on a circle omit any representation of that circle in the output
- Fixed: Modifying files in-place sometimes results in destroying non-vector files.

## 1.2.0 - 09.28.2020

- Improvement: Resort to distribution via a fat jar. Requires managing fewer files and results in a smaller installation since R8 can operate on classes from dependencies as well.
- Improvement: Use R8 for optimization. R8 produces a slightly smaller jar and in some cases faster code as well.

## 1.1.1 - 07.13.2020

- Fixed: A crash when running on a file in the current directory
- Improvement: Report an error when an input file doesn't exist

## 1.1.0 - 06.20.2020

- New: Remove redundant close path commands
- Improvement: Use the Gradle Application Plugin to build application distrobutions, simplifying installation and making running the tool a little simpler.
