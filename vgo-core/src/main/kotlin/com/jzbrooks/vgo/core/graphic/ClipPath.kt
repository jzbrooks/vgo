package com.jzbrooks.vgo.core.graphic

data class ClipPath(
    override var elements: List<Element>,
    override val id: String?,
    override val foreign: MutableMap<String, String>,
) : ContainerElement
