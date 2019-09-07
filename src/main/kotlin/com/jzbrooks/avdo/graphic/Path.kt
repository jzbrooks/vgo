package com.jzbrooks.avdo.graphic

data class Path(val data: String, val strokeWidth: Int, override val metadata: Map<String, String> = emptyMap()): Element