package com.jzbrooks.vgo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class VgoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("vgo", VgoPluginExtension::class.java)

        extension.inputs.convention(
            target.fileTree(target.layout.projectDirectory) { tree ->
                tree.include("**/res/drawable*/*.xml")
                // Build directories hold resources produced by other tasks. Consuming
                // them here creates implicit task dependencies, and in nested builds
                // those tasks belong to other projects entirely.

                // covers nested projects' conventional build directories
                tree.exclude("**/build/**")

                // covers this project's, wherever it has been relocated
                tree.exclude(BuildDirectoryExclusion(target.layout.buildDirectory))
            },
        )
        extension.showStatistics.convention(true)
        extension.format.convention(OutputFormat.UNCHANGED)
        extension.noOptimization.convention(false)
        extension.indent.convention(0)

        target.tasks.register("shrinkVectorGraphic", ShrinkVectorGraphic::class.java) { task ->
            task.inputFiles.setFrom(extension.inputs)
            // An empty output collection means "optimize in place" to the tool,
            // but the inputs must be declared as outputs for up-to-date checks
            // and build cache entries to work.
            task.outputFiles.setFrom(
                target.provider {
                    if (extension.outputs.isEmpty) extension.inputs else extension.outputs
                },
            )
            task.showStatistics.set(extension.showStatistics)
            task.outputFormat.set(extension.format)
            task.indent.set(extension.indent)
            task.noOptimization.set(extension.noOptimization)
        }

        val checkTask =
            target.tasks.register("checkVectorGraphic", CheckVectorGraphic::class.java) { task ->
                task.inputFiles.setFrom(extension.inputs)
                task.outputFormat.set(extension.format)
                task.indent.set(extension.indent)
                task.noOptimization.set(extension.noOptimization)
            }

        target.plugins.withType(LifecycleBasePlugin::class.java) {
            target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { it.dependsOn(checkTask) }
        }
    }
}
