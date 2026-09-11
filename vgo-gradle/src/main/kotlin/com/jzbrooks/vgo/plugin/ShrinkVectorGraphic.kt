package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Vgo
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path

@CacheableTask
abstract class ShrinkVectorGraphic : DefaultTask() {
    init {
        group = "resource"
        description = "Shrinks vector graphic files."
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputFiles
    abstract val outputFiles: ConfigurableFileCollection

    @get:Console
    abstract val showStatistics: Property<Boolean>

    @get:Input
    abstract val outputFormat: Property<OutputFormat>

    @get:Input
    abstract val indent: Property<Int>

    @get:Input
    abstract val noOptimization: Property<Boolean>

    // Only used to shorten paths in console output, so it can't affect the task's
    // up-to-date checks or its build cache key.
    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @TaskAction
    fun shrink() {
        val options =
            Vgo.Options(
                printVersion = false,
                printStats = showStatistics.get(),
                indent = indent.get().takeIf { it > 0 },
                output = outputFiles.files.map(File::getAbsolutePath),
                format = outputFormat.get().takeIf { it != OutputFormat.UNCHANGED }?.cliName,
                input = inputFiles.files.map(File::getAbsolutePath),
                noOptimization = noOptimization.get(),
            )

        val vgo = Vgo(options)
        val exitCode = vgo.run()

        // Reported before the failure check so a partially completed run still
        // explains the files it passed through untouched.
        val projectPath = projectDirectory.get().asFile.toPath()
        for (copy in vgo.copiedFiles) {
            logger.info(copy.describe(displayPath(projectPath, copy.output.toPath())))
        }

        if (exitCode != 0) {
            throw GradleException("vgo failed with exit code $exitCode")
        }
    }

    private fun displayPath(
        projectPath: Path,
        path: Path,
    ): String =
        if (path.startsWith(projectPath)) {
            projectPath.relativize(path).toString()
        } else {
            path.toString()
        }
}
