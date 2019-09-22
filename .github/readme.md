## Guacamole

Guacamole is a tool for optimizing vector artwork files that helps ensure your vector artwork is represented compactly without compromising quality.

## Command Line Interface

```
> guacamole [options] [file]

Options:
  -f --formatted  write the file printed writable
  -i --input      file or directory
  -o --output     file or directory, if not provided the input will be overwritten
  -d --directory  optimizes and rewrite all files in a directory
```

## Build instructions

This project uses the Gradle build system.

To run the application: `/.gradlew run --args="./path/to/file.xml"`

To run the tests: `./gradlew test`

To see all available tasks: `./gradlew tasks`
