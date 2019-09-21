package com.jzbrooks.guacamole.graphic

data class Group(override var elements: List<Element>, override var attributes: Map<String, String> = emptyMap()): ContainerElement