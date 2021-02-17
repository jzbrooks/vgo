package com.jzbrooks.vgo.plugin

import org.gradle.api.file.FileTree

open class VgoPluginExtension {
    var inputs: FileTree? = null
    var outputs: FileTree? = inputs
    var showStatistics = true
    var format = OutputFormat.UNCHANGED
    var indent: Byte = 0
}
