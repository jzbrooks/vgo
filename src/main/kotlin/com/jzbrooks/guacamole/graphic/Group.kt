package com.jzbrooks.guacamole.graphic

import com.jzbrooks.guacamole.util.math.Point

data class Group(
        override var elements: List<Element>,
        override var attributes: Map<String, String> = emptyMap(),
        var scale: Point? = null,
        var translation: Point? = null,
        var pivot: Point? = null,
        var rotation: Float? = null
    ): ContainerElement