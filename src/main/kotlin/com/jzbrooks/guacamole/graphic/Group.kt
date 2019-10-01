package com.jzbrooks.guacamole.graphic

import com.jzbrooks.guacamole.util.math.Matrix3

data class Group(
        override var elements: List<Element>,
        override var attributes: Map<String, String> = emptyMap(),
        var transform: Matrix3? = null
    ): ContainerElement