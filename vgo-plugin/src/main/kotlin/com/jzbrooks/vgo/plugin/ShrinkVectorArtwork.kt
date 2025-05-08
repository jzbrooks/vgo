package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Vgo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.File

open class ShrinkVectorArtwork : DefaultTask() {
    private val extension = project.extensions.getByType<VgoPluginExtension>()

    private val defaultTree =
        project.fileTree(project.projectDir) {
            include("**/res/drawable*/*.xml")
        }

    init {
        group = "resource"
        description = "Shrink vector resources."
    }

    @get:Input
    val files: List<String> = (extension.inputs ?: defaultTree).files.map(File::getAbsolutePath)

    @get:OutputFiles
    val outputFiles: List<String> = (extension.outputs ?: extension.inputs)?.files.orEmpty().map(File::getAbsolutePath)

    @get:Input
    val showStatistics = extension.showStatistics

    @get:Input
    val outputFormat = extension.format

    @get:Input
    val indent = extension.indent

    @get:Input
    val noOptimization = extension.noOptimization

    @TaskAction
    fun shrink() {
        logger.lifecycle("Extension outputs: ${extension.outputs?.files}")
        logger.lifecycle("Outputs: $outputFiles")

        for (file in extension.outputs?.files ?: emptyList()) {
            file.mkdirs()
        }

        val options =
            Vgo.Options(
                printVersion = false,
                printStats = showStatistics,
                indent = indent.takeIf { it > 0 }?.toInt(),
                output = outputFiles,
                format = outputFormat.cliName,
                input = files,
                noOptimization = noOptimization,
            )

        Vgo(options).run()
    }
}
