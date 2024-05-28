package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.svg.ScalableVectorGraphic

fun VectorDrawable.toSvg(): ScalableVectorGraphic {
    val viewportHeight = foreign.remove("android:viewportHeight") ?: "24"
    val viewportWidth = foreign.remove("android:viewportWidth") ?: "24"

    val svgElementAttributes =
        mutableMapOf(
            "xmlns" to "http://www.w3.org/2000/svg",
            "viewPort" to "0 0 $viewportWidth $viewportHeight",
            "width" to "100%",
            "height" to "100%",
        )

    traverse(this)
    foreign.clear()

    return ScalableVectorGraphic(elements, id, svgElementAttributes)
}

private fun traverse(element: Element): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map(::traverse)
    }

    element.foreign.clear()

    return element
}
