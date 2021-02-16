package com.jzbrooks.vgo.plugin

import org.gradle.api.file.FileTree

enum class OutputFormat(internal val cliName: String) {
    SVG("svg"),
    VECTOR_DRAWABLE("vd"),
    UNCHANGED("unchanged"),
}

open class VgoPluginExtension {
    var inputs: FileTree? = null
    var outputs: FileTree? = inputs
    var showStatistics = true
    var format = OutputFormat.UNCHANGED
    var indent: Byte = 0
}
