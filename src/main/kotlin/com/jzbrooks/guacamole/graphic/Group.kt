package com.jzbrooks.guacamole.graphic

data class Group(override var elements: List<Element>, override var metadata: Map<String, String> = emptyMap()): ContainerElement