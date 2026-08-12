package com.jzbrooks.vgo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

class VgoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("vgo", VgoPluginExtension::class.java)

        // Rooted at the conventional source directory rather than the project
        // directory. Sweeping the whole project picks up resources generated into
        // build directories and, when this project contains nested ones, their
        // sources too — both of which belong to other tasks. Projects that keep
        // artwork elsewhere configure `inputs` themselves.
        extension.inputs.convention(
            target.fileTree(target.layout.projectDirectory.dir("src")) { tree ->
                tree.include("**/res/drawable*/*.xml")
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
            // and build cache entries to work. These are resolved to plain files
            // rather than passed along as a tree, because a tree declares its
            // root directory as the output location and the filters are ignored.
            task.outputFiles.setFrom(
                target.provider {
                    if (extension.outputs.isEmpty) extension.inputs.files else extension.outputs.files
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
