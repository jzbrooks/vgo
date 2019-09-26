## Guacamole

Guacamole is a tool for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.

## Command Line Interface

```
> guacamole [options] [file/directory]

Options:
  -h --help       print this message
  -o --output     file or directory, if not provided the input will be overwritten
  -s --stats      print statistics on processed files to standard out
  -v --version    print the version number
  --indent=value  write files with value columns of indentation  
```

## Build instructions

This project uses the Gradle build system.

To run the application: `/.gradlew run --args="./path/to/file.xml"`

To run the tests: `./gradlew test`

To see all available tasks: `./gradlew tasks`
