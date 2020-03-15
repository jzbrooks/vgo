package com.jzbrooks.vgo.svg.graphic

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element

data class ClipPath(
        override var elements: List<Element>,
        override val attributes: MutableMap<String, String> = mutableMapOf()
) : ContainerElement