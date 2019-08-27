package com.jzbrooks.avdo.graphic

data class Group(val paths: List<Path>, override val metadata: Map<String, String> = emptyMap()): Element