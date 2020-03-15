package com.jzbrooks.vgo.core.graphic

data class Group(
    override var elements: List<Element>,
    override val attributes: MutableMap<String, String> = mutableMapOf()
): ContainerElement