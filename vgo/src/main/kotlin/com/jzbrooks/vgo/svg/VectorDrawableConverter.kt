package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.svg.graphic.ClipPath
import com.jzbrooks.vgo.vd.VectorDrawable
import kotlin.math.atan
import kotlin.math.hypot
import com.jzbrooks.vgo.vd.graphic.ClipPath as AndroidClipPath

private val namedColorValues = mapOf(
        "black" to "#000000",
        "silver" to "#c0c0c0",
        "gray" to "#808080",
        "white" to "#ffffff",
        "maroon" to "#800000",
        "red" to "#ff0000",
        "purple" to "#800080",
        "fuchsia" to "#ff00ff",
        "magenta" to "#ff00ff",
        "green" to "#008000",
        "lime" to "#00ff00",
        "olive" to "#808000",
        "yellow" to "#ffff00",
        "navy" to "#000080",
        "blue" to "#0000ff",
        "teal" to "#008080",
        "aqua" to "#00ffff",
        "cyan" to "#00ffff",
        "orange" to "#ffa500",
        "aliceblue" to "#f0f8ff",
        "antiquewhite" to "#faebd7",
        "aquamarine" to "#7fffd4",
        "azure" to "#f0ffff",
        "beige" to "#f5f5dc",
        "bisque" to "#ffe4c4",
        "blanchedalmond" to "#ffebcd",
        "blueviolet" to "#8a2be2",
        "brown" to "#a52a2a",
        "burlywood" to "#deb887",
        "cadetblue" to "#5f9ea0",
        "chartreuse" to "#7fff00",
        "chocolate" to "#d2691e",
        "coral" to "#ff7f50",
        "cornflowerblue" to "#6495ed",
        "cornsilk" to "#fff8dc",
        "crimson" to "#dc143c",
        "darkblue" to "#00008b",
        "darkcyan" to "#008b8b",
        "darkgoldenrod" to "#b8860b",
        "darkgray" to "#a9a9a9",
        "darkgreen" to "#006400",
        "darkgrey" to "#a9a9a9",
        "darkkhaki" to "#bdb76b",
        "darkmagenta" to "#8b008b",
        "darkolivegreen" to "#556b2f",
        "darkorange" to "#ff8c00",
        "darkorchid" to "#9932cc",
        "darkred" to "#8b0000",
        "darksalmon" to "#e9967a",
        "darkseagreen" to "#8fbc8f",
        "darkslateblue" to "#483d8b",
        "darkslategray" to "#2f4f4f",
        "darkslategrey" to "#2f4f4f",
        "darkturquoise" to "#00ced1",
        "darkviolet" to "#9400d3",
        "deeppink" to "#ff1493",
        "deepskyblue" to "#00bfff",
        "dimgray" to "#696969",
        "dimgrey" to "#696969",
        "dodgerblue" to "#1e90ff",
        "firebrick" to "#b22222",
        "floralwhite" to "#fffaf0",
        "forestgreen" to "#228b22",
        "gainsboro" to "#dcdcdc",
        "ghostwhite" to "#f8f8ff",
        "gold" to "#ffd700",
        "goldenrod" to "#daa520",
        "greenyellow" to "#adff2f",
        "grey" to "#808080",
        "honeydew" to "#f0fff0",
        "hotpink" to "#ff69b4",
        "indianred" to "#cd5c5c",
        "indigo" to "#4b0082",
        "ivory" to "#fffff0",
        "khaki" to "#f0e68c",
        "lavender" to "#e6e6fa",
        "lavenderblush" to "#fff0f5",
        "lawngreen" to "#7cfc00",
        "lemonchiffon" to "#fffacd",
        "lightblue" to "#add8e6",
        "lightcoral" to "#f08080",
        "lightcyan" to "#e0ffff",
        "lightgoldenrodyellow" to "#fafad2",
        "lightgray" to "#d3d3d3",
        "lightgreen" to "#90ee90",
        "lightgrey" to "#d3d3d3",
        "lightpink" to "#ffb6c1",
        "lightsalmon" to "#ffa07a",
        "lightseagreen" to "#20b2aa",
        "lightskyblue" to "#87cefa",
        "lightslategray" to "#778899",
        "lightslategrey" to "#778899",
        "lightsteelblue" to "#b0c4de",
        "lightyellow" to "#ffffe0",
        "limegreen" to "#32cd32",
        "linen" to "#faf0e6",
        "mediumaquamarine" to "#66cdaa",
        "mediumblue" to "#0000cd",
        "mediumorchid" to "#ba55d3",
        "mediumpurple" to "#9370db",
        "mediumseagreen" to "#3cb371",
        "mediumslateblue" to "#7b68ee",
        "mediumspringgreen" to "#00fa9a",
        "mediumturquoise" to "#48d1cc",
        "mediumvioletred" to "#c71585",
        "midnightblue" to "#191970",
        "mintcream" to "#f5fffa",
        "mistyrose" to "#ffe4e1",
        "moccasin" to "#ffe4b5",
        "navajowhite" to "#ffdead",
        "oldlace" to "#fdf5e6",
        "olivedrab" to "#6b8e23",
        "orangered" to "#ff4500",
        "orchid" to "#da70d6",
        "palegoldenrod" to "#eee8aa",
        "palegreen" to "#98fb98",
        "paleturquoise" to "#afeeee",
        "palevioletred" to "#db7093",
        "papayawhip" to "#ffefd5",
        "peachpuff" to "#ffdab9",
        "peru" to "#cd853f",
        "pink" to "#ffc0cb",
        "plum" to "#dda0dd",
        "powderblue" to "#b0e0e6",
        "rosybrown" to "#bc8f8f",
        "royalblue" to "#4169e1",
        "saddlebrown" to "#8b4513",
        "salmon" to "#fa8072",
        "sandybrown" to "#f4a460",
        "seagreen" to "#2e8b57",
        "seashell" to "#fff5ee",
        "sienna" to "#a0522d",
        "skyblue" to "#87ceeb",
        "slateblue" to "#6a5acd",
        "slategray" to "#708090",
        "slategrey" to "#708090",
        "snow" to "#fffafa",
        "springgreen" to "#00ff7f",
        "steelblue" to "#4682b4",
        "tan" to "#d2b48c",
        "thistle" to "#d8bfd8",
        "tomato" to "#ff6347",
        "turquoise" to "#40e0d0",
        "violet" to "#ee82ee",
        "wheat" to "#f5deb3",
        "whitesmoke" to "#f5f5f5",
        "yellowgreen" to "#9acd32",
        "rebeccapurple"	to "#663399"
)

private val attributeNames = mapOf(
        "id" to "android:name",
        "fill" to "android:fillColor",
        "fill-rule" to "android:fillType",
        "fill-opacity" to "android:fillAlpha",
        "stroke" to "android:strokeColor",
        "stroke-width" to "android:strokeWidth",
        "stroke-opacity" to "android:strokeAlpha",
        "stroke-linecap" to "android:strokeLineCap",
        "stroke-linejoin" to "android:strokeLineJoin",
        "stroke-miterlimit" to "android:strokeMiterLimit"
)

fun ScalableVectorGraphic.toVectorDrawable(): VectorDrawable {
    val graphic = traverse(this) as ContainerElement
    return VectorDrawable(graphic.elements, convertTopLevelAttributes(attributes))
}

private fun traverse(element: Element): Element {
    return when (element) {
        is ContainerElement -> process(element)
        is PathElement -> process(element)
        else -> element
    }
}

private fun process(containerElement: ContainerElement): Element {
    val clipPaths = containerElement.elements
            .filterIsInstance<ClipPath>()
            .associateBy { it.attributes.getValue("id") }

    val newElements = mutableListOf<Element>()
    for (element in containerElement.elements.filter { it !is ClipPath }) {
        if (element.attributes.containsKey("clip-path")) {
            val id = element.attributes
                    .remove("clip-path")!!
                    .removePrefix("url(#")
                    .trimEnd(')')

            val clip = clipPaths.getValue(id)
            val vdClipPaths = clip.elements
                    .filterIsInstance<Path>()
                    .map { AndroidClipPath(it.commands, it.attributes) }

            // I'm not sure grouping clip paths like this
            // is a very good long-term solution, but it works
            // for relatively simple cases.
            val group = mutableListOf<Element>()
            group.addAll(vdClipPaths)
            group.add(element)
            newElements.add(Group(group))
        } else if (element !is ClipPath) {
            newElements.add(element)
        }
    }

    val newAttributes = convertContainerElementAttributes(containerElement.attributes)
    containerElement.attributes.putAll(newAttributes)

    return containerElement.apply { elements = newElements.map(::traverse) }
}

private fun process(pathElement: PathElement): Element {
    return pathElement.apply {
        val newElements = convertPathElementAttributes(attributes)
        attributes.putAll(newElements)
    }
}

private fun convertPathElementAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    val vdPathElementAttributes = mutableMapOf<String, String>()

    vdPathElementAttributes.putAll(mapAttributes(attributes))
    vdPathElementAttributes.putIfAbsent("android:strokeWidth", "1")

    return vdPathElementAttributes
}

private fun convertContainerElementAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    val vdContainerElements = mutableMapOf<String, String>()

    attributes.remove("transform")?.let { matrix ->
        val entries = matrix.removePrefix("matrix(")
                .trimEnd(')')
                .split(',')
                .map { it.trim() }

        val a = entries[0].toDouble()
        val b = entries[2].toDouble()
        val c = entries[1].toDouble()
        val d = entries[3].toDouble()

        if (entries[4] != "0") {
            vdContainerElements["android:translateX"] = entries[4]
        }

        if (entries[5] != "0") {
            vdContainerElements["android:translateY"] = entries[5]
        }

        // todo(jzb): truncate at some precision
        // todo(jzb): compare floats with some epsilon
        val scaleX = hypot(a, c)
        if (scaleX != 1.0) {
            vdContainerElements["android:scaleX"] = scaleX.toString()
        }

        val scaleY = hypot(b, d)
        if (scaleY != 1.0) {
            vdContainerElements["android:scaleY"] = scaleY.toString()
        }

        val rotation = atan(c/d)
        if (rotation != 0.0) {
            vdContainerElements["android:rotation"] = rotation.toString()
        }
    }

    vdContainerElements.putAll(mapAttributes(attributes))

    return vdContainerElements
}

private fun convertTopLevelAttributes(attributes: MutableMap<String, String>): MutableMap<String, String> {
    attributes.remove("xmlns")

    val viewBox = attributes.remove("viewBox")!!.split(" ")

    val width = run {
        val w = attributes.remove("width")
        if (w != null && !w.endsWith('%')) {
            w
        } else {
            viewBox[2]
        }
    }

    val height = run {
        val h = attributes.remove("height")
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

    vdElementAttributes.putAll(mapAttributes(attributes))

    return vdElementAttributes
}

private fun mapAttributes(attributes: MutableMap<String, String>): Map<String, String> {
    val newAttributes = mutableMapOf<String, String>()

    fun addAttribute(key: String, value: String) {
        val newValue = when (key) {
            "fill", "stroke" -> when (value) {
                "none" -> return
                else -> mapColor(value)
            }
            else -> value
        }

        val newKey = attributeNames[key] ?: key

        newAttributes[newKey] = newValue
    }

    for ((key, value) in attributes) {
        if (key == "style") {
            value.split(';')
                    .map { it.split(":") }
                    .filter { it.size == 2 }
                    .forEach { (key, value) ->
                        addAttribute(key, value)
                    }
        } else {
            addAttribute(key, value)
        }
    }

    // We've mangled the map at this point...
    attributes.clear()

    return newAttributes
}

private fun mapColor(color: String): String {
    return if (color.startsWith("rgb")) {
        val (r, g, b) = color.removePrefix("rgb(")
                .trimEnd(')')
                .split(',')
                .map { it.trim().toShort() }

        "#%02x%02x%02x".format(r, g, b)
    } else {
        namedColorValues[color] ?: color
    }
}