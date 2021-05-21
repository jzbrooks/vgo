package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.vd.VectorDrawable

fun ScalableVectorGraphic.toVectorDrawable(): VectorDrawable {
    val viewBox = foreign.remove("viewBox")!!.split(" ")

    val width = run {
        val w = foreign.remove("width")
        if (w != null && !w.endsWith('%')) {
            w
        } else {
            viewBox[2]
        }
    }

    val height = run {
        val h = foreign.remove("height")
        if (h != null && !h.endsWith('%')) {
            h
        } else {
            viewBox[3]
        }
    }

    val vdElementAttributes = mutableMapOf(
        "xmlns:android" to "http://schemas.android.com/apk/res/android",
        "android:viewportWidth" to viewBox[2],
        "android:viewportHeight" to viewBox[3],
        "android:width" to "${width}dp",
        "android:height" to "${height}dp"
    )

    traverse(this)
    foreign.clear()

    return VectorDrawable(elements, id, vdElementAttributes)
}

private fun traverse(element: Element): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map(::traverse)
    }

    element.foreign.clear()

    return element
}
