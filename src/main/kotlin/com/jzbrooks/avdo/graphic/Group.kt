package com.jzbrooks.avdo.graphic

data class Group(var paths: List<PathElement>, override var metadata: Map<String, String> = emptyMap()): Element