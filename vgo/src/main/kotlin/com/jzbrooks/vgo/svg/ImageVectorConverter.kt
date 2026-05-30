package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.iv.ImageVector

@ExperimentalVgoApi
fun ScalableVectorGraphic.toImageVector(): ImageVector {
    val viewBox = foreign.remove("viewBox")!!.split(" ")

    val width =
        run {
            val w = foreign.remove("width")
            if (w != null && !w.endsWith('%')) {
                w
            } else {
                viewBox[2]
            }
        }

    val height =
        run {
            val h = foreign.remove("height")
            if (h != null && !h.endsWith('%')) {
                h
            } else {
                viewBox[3]
            }
        }

    traverse(this)
    foreign.clear()

    return ImageVector(
        elements,
        id,
        mutableMapOf(),
        width.toFloat(),
        height.toFloat(),
        viewBox[2].toFloat(),
        viewBox[3].toFloat(),
    )
}

private fun traverse(element: Element): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map(::traverse)
    }

    element.foreign.clear()

    return element
}
