package com.jzbrooks.avdo.graphic

data class Group(var paths: List<Path>, override var metadata: Map<String, String> = emptyMap()): Element