package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Vgo
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class ShrinkVectorGraphic : DefaultTask() {
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

        val exitCode = Vgo(options).run()
        if (exitCode != 0) {
            throw GradleException("vgo failed with exit code $exitCode")
        }
    }
}
