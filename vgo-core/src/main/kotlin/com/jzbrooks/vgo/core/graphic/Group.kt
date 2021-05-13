package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.util.math.Matrix3

data class Group(
    override var elements: List<Element>,
    override val id: String? = null,
    override val foreign: MutableMap<String, String> = mutableMapOf(),
    var transform: Matrix3 = Matrix3.IDENTITY,
) : ContainerElement
