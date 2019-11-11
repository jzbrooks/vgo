package com.jzbrooks.guacamole.svg.graphic

import com.jzbrooks.guacamole.core.graphic.ContainerElement
import com.jzbrooks.guacamole.core.graphic.Element

data class ClipPath(
        override var elements: List<Element>,
        override val attributes: MutableMap<String, String> = mutableMapOf()
) : ContainerElement