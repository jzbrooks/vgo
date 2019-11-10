## Guacamole

Guacamole is a tool (similar to [avocado](https://github.com/alexjlockwood/avocado) and [svgo](https://github.com/svg/svgo)) for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.

[![Build Status](https://github.com/jzbrooks/guacamole/workflows/build/badge.svg)](https://github.com/jzbrooks/guacamole/actions?workflow=build)

## Command Line Interface

```
> guacamole [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent [value]  write files with value columns of indentation  
```

## Examples

```
# Optimize files specified from standard in
> find ./**/ic_*.xml | guacamole

# Optimize vector.xml and overwrite its contents
> guacamole vector.xml

# Optimize vector.xml and write the result into new_vector.xml
> guacamole vector.xml -o new_vector.xml

# Optimize multiple input sources write results to the
> guacamole vector.xml -o new_vector.xml ./assets -o ./new_assets
```

## Build instructions

This project uses the Gradle build system.

To build the application: `/.gradlew jar`

To run the tests: `./gradlew check`

To see all available tasks: `./gradlew tasks`

![guacamole](https://user-images.githubusercontent.com/5142575/68550403-2f4adf80-03c8-11ea-8bd8-f845cd857940.png)
