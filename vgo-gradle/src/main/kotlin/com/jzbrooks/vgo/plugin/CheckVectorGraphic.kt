package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Vgo
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Verification task with no outputs")
abstract class CheckVectorGraphic : DefaultTask() {
    init {
        group = "verification"
        description = "Verifies vector graphic files are fully shrunk."
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val outputFormat: Property<OutputFormat>

    @get:Input
    abstract val indent: Property<Int>

    @get:Input
    abstract val noOptimization: Property<Boolean>

    @TaskAction
    fun check() {
        val format = outputFormat.get()
        if (format != OutputFormat.UNCHANGED) {
            throw GradleException(
                "checkVectorGraphic does not support format conversion (vgo.format = $format). " +
                    "Check mode verifies files in their current format.",
            )
        }

        val options =
            Vgo.Options(
                indent = indent.get().takeIf { it > 0 },
                input = inputFiles.files.map(File::getAbsolutePath),
                noOptimization = noOptimization.get(),
                checkOnly = true,
            )

        val exitCode = Vgo(options).run()
        if (exitCode != 0) {
            throw GradleException(
                "Vector graphics are not fully shrunk (see the file list above). " +
                    "Run the shrinkVectorGraphic task to fix them.",
            )
        }
    }
}
