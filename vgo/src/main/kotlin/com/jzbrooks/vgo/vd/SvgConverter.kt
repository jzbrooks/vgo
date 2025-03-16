package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.composable.ImageVectorGraphic
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

fun VectorDrawable.toImageVector(): ImageVectorGraphic {
    val width = foreign.remove("android:width")?.removeSuffix("dp") ?: "24"
    val height = foreign.remove("android:height")?.removeSuffix("dp") ?: "24"
    val viewportHeight = foreign.remove("android:viewportHeight") ?: "24"
    val viewportWidth = foreign.remove("android:viewportWidth") ?: "24"

    val imageVectorElementAttributes =
        mutableMapOf(
            "viewportWidth" to viewportWidth,
            "viewportHeight" to viewportHeight,
            "defaultWidth" to width,
            "defaultHeight" to height,
        )

    traverse(this)
    foreign.clear()

    return ImageVectorGraphic(elements, id, imageVectorElementAttributes, id ?: "image", null)
}

private fun traverse(element: Element): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map(::traverse)
    }

    element.foreign.clear()

    return element
}
