package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Vgo
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Verification task with no outputs")
open class CheckVectorGraphic : DefaultTask() {
    private val extension = project.extensions.getByType(VgoPluginExtension::class.java)

    init {
        group = "verification"
        description = "Verifies vector graphic files are fully shrunk."
    }

    @get:Input
    val files: List<String> = extension.inputs.files.map(File::getAbsolutePath)

    @get:Input
    val outputFormat = extension.format

    @get:Input
    val indent = extension.indent

    @get:Input
    val noOptimization = extension.noOptimization

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
                input = files,
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
