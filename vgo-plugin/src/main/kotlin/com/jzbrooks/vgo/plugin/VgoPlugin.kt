package com.jzbrooks.vgo.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class VgoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create<VgoPluginExtension>("vgo")

        target.tasks.register<ShrinkVectorArtwork>("shrinkVectorArtwork") {
            group = "resource"
            description = "Shrinks vector graphic files. Deprecated. Use shrinkVectorGraphic instead."
            doFirst {
                logger.warn("This task is deprecated. Use shrinkVectorGraphic instead.")
            }
        }

        target.tasks.register<ShrinkVectorGraphic>("shrinkVectorGraphic") {
            group = "resource"
            description = "Shrinks vector graphic files."
        }
    }
}
