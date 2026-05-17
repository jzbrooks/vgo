package com.jzbrooks.vgo.plugin

import org.gradle.api.file.FileTree

open class VgoPluginExtension {
    var inputs: FileTree? = null
    var outputs: FileTree? = null
    var showStatistics = true
    var format = OutputFormat.UNCHANGED
    var noOptimization = false
    var indent: Byte = 0
}
