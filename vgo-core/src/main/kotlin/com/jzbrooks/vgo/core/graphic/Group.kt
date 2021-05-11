package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.util.math.Matrix3

data class Group(
    override var elements: List<Element>,
    override val attributes: GroupAttributes = Attributes(null, Matrix3.IDENTITY, mutableMapOf()), // todo(maybe): remove the default parameter?
) : ContainerElement {

    data class Attributes(
        override val id: String?,
        override var transform: Matrix3,
        override val foreign: MutableMap<String, String>,
    ) : GroupAttributes {

        override fun isEmpty() = id == null && transform === Matrix3.IDENTITY && foreign.isEmpty()
    }
}
