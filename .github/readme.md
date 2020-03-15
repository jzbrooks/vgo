## vgo

vgo is a tool for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.

[![Build Status](https://github.com/jzbrooks/vgo/workflows/build/badge.svg)](https://github.com/jzbrooks/vgo/actions?workflow=build)

## Command Line Interface

```
> vgo [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent [value]  write files with value columns of indentation
  --format [value]  output format (svg, vd (coming soon), etc)  
```

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

To build the application: `/.gradlew jar`

To run the tests: `./gradlew check`

To see all available tasks: `./gradlew tasks`
