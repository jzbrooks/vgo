package com.jzbrooks.vgo.plugin

import com.jzbrooks.vgo.Application
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.kotlin.dsl.create

class VgoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create<VgoPluginExtension>("vgo")

        val defaultTree: FileTree = target.fileTree(target.projectDir) {
            include("**/res/drawable*/*.xml")
        }

        target.tasks.register("shrinkVectorArtwork") {
            val argList = mutableListOf<String>()
            val input = (extension.inputs ?: defaultTree).files

            argList.addAll(input.map { it.absolutePath })

            if (extension.indent != 0.toByte()) {
                argList.addAll(arrayOf("--indent", extension.indent.toString()))
            }

            if (extension.format != OutputFormat.UNCHANGED) {
                argList.addAll(arrayOf("--format", extension.format.cliName))
            }

            if (extension.showStatistics) {
                argList.add("--stats")
            }

            group = "resource"
            description = "Shrink vector resources."

            inputs.files(input)
            outputs.files(input)

            doLast {
                Application().run(argList.toTypedArray())
            }
        }
    }
}