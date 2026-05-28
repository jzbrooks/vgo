package com.jzbrooks.vgo.core.graphic

data class ClipPath(
    var regions: List<Path>,
    val id: String? = null,
    val foreign: MutableMap<String, String> = mutableMapOf(),
)
