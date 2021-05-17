package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.PathElement
import com.jzbrooks.vgo.svg.ScalableVectorGraphic
import com.jzbrooks.vgo.vd.graphic.ClipPath

private val hexWithAlpha = Regex("#[a-fA-F\\d]{8}")

fun VectorDrawable.toSvg(): ScalableVectorGraphic {
    val graphic = traverse(this) as ContainerElement

    val viewportHeight = foreign.remove("android:viewportHeight") ?: "24"
    val viewportWidth = foreign.remove("android:viewportWidth") ?: "24"
    foreign.remove("xmlns:android")

    val svgElementAttributes = mutableMapOf(
        "xmlns" to "http://www.w3.org/2000/svg",
        "viewPort" to "0 0 $viewportWidth $viewportHeight"
    )

    svgElementAttributes["width"] = "100%"
    svgElementAttributes["height"] = "100%"

    return ScalableVectorGraphic(graphic.elements, id, svgElementAttributes)
}

private fun traverse(element: Element): Element {
    return when (element) {
        is ContainerElement -> process(element)
        is PathElement -> process(element)
        else -> element
    }
}

private fun process(containerElement: ContainerElement): Element {
    val newElements = mutableListOf<Element>()
    var defs: Extra? = null

    for ((index, element) in containerElement.elements.withIndex()) {
        if (element !is ClipPath) {
            if (defs != null) {
                element.foreign["clip-path"] = "url(#${defs.id})"
            }
            newElements.add(traverse(element))
        } else {
            defs = Extra(
                "defs",
                listOf(
                    Path(
                        element.commands,
                        null,
                        mutableMapOf(),
                        Colors.BLACK,
                        Colors.TRANSPARENT,
                        1f,
                        Path.LineCap.BUTT,
                    )
                ),
                "clip_$index",
                mutableMapOf(),
            )
        }
    }

    if (defs != null) {
        newElements.add(defs)
    }

    return containerElement.apply { elements = newElements }
}

private fun process(pathElement: PathElement): Element {
    return pathElement.apply {
        val newElements = convertPathElementAttributes(foreign.toMutableMap())
        foreign.clear()
        foreign.putAll(newElements)
    }
}

private fun convertPathElementAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    val svgPathElementAttributes = mutableMapOf<String, String>()

    for ((key, value) in attributes) {

        var newValue = value
        if (hexWithAlpha.matches(value)) {
            newValue = '#' + value.substring(3)
        }

        val svgKey = when (key) {
            "android:fillColor" -> "fill"
            "android:fillType" -> "fill-rule"
            "android:fillAlpha" -> "fill-opacity"
            // todo: figure out what to do with these
            // "android:trimPathStart" -> ""
            // "android:trimPathEnd" -> ""
            // "android:trimPathOffset" -> ""
            "android:strokeColor" -> "stroke"
            "android:strokeWidth" -> "stroke-width"
            "android:strokeAlpha" -> "stroke-opacity"
            "android:strokeLineCap" -> "stroke-linecap"
            "android:strokeLineJoin" -> "stroke-linejoin"
            "android:strokeMiterLimit" -> "stroke-miterlimit"
            else -> key
        }

        svgPathElementAttributes[svgKey] = newValue
    }

    // We've mangled the map at this point...
    attributes.clear()

    return svgPathElementAttributes
}
