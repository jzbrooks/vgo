package com.jzbrooks.vgo.plugin

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property

abstract class VgoPluginExtension {
    abstract val inputs: ConfigurableFileCollection
    abstract val outputs: ConfigurableFileCollection
    abstract val showStatistics: Property<Boolean>
    abstract val format: Property<OutputFormat>
    abstract val noOptimization: Property<Boolean>
    abstract val indent: Property<Int>
}
