package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Application
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
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

    @get:Input
    val showStatistics = extension.showStatistics

    @get:Input
    val outputFormat = extension.format

    @get:Input
    val indent = extension.indent

    @TaskAction
    fun shrink() {
        val options = Application.Options(
            printHelp = false,
            printVersion = false,
            printStats = showStatistics,
            indent = indent.takeIf { it > 0 }?.toInt(),
            output = emptyList(),
            format = outputFormat.cliName,
            input = files
        )

        Application(options).run()
    }
}
