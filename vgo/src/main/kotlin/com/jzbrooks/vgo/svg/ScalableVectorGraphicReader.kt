package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.xml.asSequence
import com.jzbrooks.vgo.core.util.xml.removeOrNull
import com.jzbrooks.vgo.core.util.xml.toMutableMap
import com.jzbrooks.vgo.svg.graphic.ClipPath
import org.w3c.dom.Comment
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.Text

fun parse(root: Node): ScalableVectorGraphic {
    val elements = root.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return ScalableVectorGraphic(
        elements,
        root.attributes.removeOrNull("id")?.nodeValue,
        root.attributes.toMutableMap(),
    )
}

private fun parseElement(node: Node): Element? {
    if (node is Text || node is Comment) return null

    return when (node.nodeName) {
        "g" -> parseGroupElement(node)
        "clipPath" -> parseClipPath(node)
        "path" -> parsePathElement(node)
        else -> parseExtraElement(node)
    }
}

private fun parseClipPath(node: Node): ClipPath {
    val childElements = node.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return ClipPath(
        childElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun parseGroupElement(node: Node): Group {
    val childElements = node.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    // This has to happen before foreign property collection
    val transform = node.attributes.extractTransformMatrix()
    return Group(
        childElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
        transform,
    )
}

private fun parsePathElement(node: Node): Path {
    val commands = CommandString(node.attributes.removeNamedItem("d").nodeValue.toString()).toCommandList()
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val fill = node.attributes.extractColor("fill", Colors.BLACK)
    val stroke = node.attributes.extractColor("stroke", Colors.TRANSPARENT)
    val strokeWidth = node.attributes.removeOrNull("stroke-width")?.nodeValue?.toUIntOrNull() ?: 1u

    return Path(
        commands,
        id,
        node.attributes.toMutableMap(),
        fill,
        stroke,
        strokeWidth,
    )
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return Extra(
        node.nodeValue ?: node.nodeName,
        containedElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun NamedNodeMap.extractTransformMatrix(): Matrix3 {
    val transform = removeOrNull("transform")?.nodeValue ?: return Matrix3.IDENTITY

    val entries = transform.removePrefix("matrix(")
        .trimEnd(')')
        .split(',')
        .map(String::toFloat)

    return Matrix3.from(
        arrayOf(
            floatArrayOf(entries[0], entries[2], entries[4]),
            floatArrayOf(entries[1], entries[3], entries[5]),
            floatArrayOf(0f, 0f, 1f),
        )
    )
}

// todo(optimization): Make this Map<String, Color>
private val NAMED_COLORS = mapOf(
    "black" to "000000",
    "silver" to "c0c0c0",
    "gray" to "808080",
    "white" to "ffffff",
    "maroon" to "800000",
    "red" to "ff0000",
    "purple" to "800080",
    "fuchsia" to "ff00ff",
    "magenta" to "ff00ff",
    "green" to "008000",
    "lime" to "00ff00",
    "olive" to "808000",
    "yellow" to "ffff00",
    "navy" to "000080",
    "blue" to "0000ff",
    "teal" to "008080",
    "aqua" to "00ffff",
    "cyan" to "00ffff",
    "orange" to "ffa500",
    "aliceblue" to "f0f8ff",
    "antiquewhite" to "faebd7",
    "aquamarine" to "7fffd4",
    "azure" to "f0ffff",
    "beige" to "f5f5dc",
    "bisque" to "ffe4c4",
    "blanchedalmond" to "ffebcd",
    "blueviolet" to "8a2be2",
    "brown" to "a52a2a",
    "burlywood" to "deb887",
    "cadetblue" to "5f9ea0",
    "chartreuse" to "7fff00",
    "chocolate" to "d2691e",
    "coral" to "ff7f50",
    "cornflowerblue" to "6495ed",
    "cornsilk" to "fff8dc",
    "crimson" to "dc143c",
    "darkblue" to "00008b",
    "darkcyan" to "008b8b",
    "darkgoldenrod" to "b8860b",
    "darkgray" to "a9a9a9",
    "darkgreen" to "006400",
    "darkgrey" to "a9a9a9",
    "darkkhaki" to "bdb76b",
    "darkmagenta" to "8b008b",
    "darkolivegreen" to "556b2f",
    "darkorange" to "ff8c00",
    "darkorchid" to "9932cc",
    "darkred" to "8b0000",
    "darksalmon" to "e9967a",
    "darkseagreen" to "8fbc8f",
    "darkslateblue" to "483d8b",
    "darkslategray" to "2f4f4f",
    "darkslategrey" to "2f4f4f",
    "darkturquoise" to "00ced1",
    "darkviolet" to "9400d3",
    "deeppink" to "ff1493",
    "deepskyblue" to "00bfff",
    "dimgray" to "696969",
    "dimgrey" to "696969",
    "dodgerblue" to "1e90ff",
    "firebrick" to "b22222",
    "floralwhite" to "fffaf0",
    "forestgreen" to "228b22",
    "gainsboro" to "dcdcdc",
    "ghostwhite" to "f8f8ff",
    "gold" to "ffd700",
    "goldenrod" to "daa520",
    "greenyellow" to "adff2f",
    "grey" to "808080",
    "honeydew" to "f0fff0",
    "hotpink" to "ff69b4",
    "indianred" to "cd5c5c",
    "indigo" to "4b0082",
    "ivory" to "fffff0",
    "khaki" to "f0e68c",
    "lavender" to "e6e6fa",
    "lavenderblush" to "fff0f5",
    "lawngreen" to "7cfc00",
    "lemonchiffon" to "fffacd",
    "lightblue" to "add8e6",
    "lightcoral" to "f08080",
    "lightcyan" to "e0ffff",
    "lightgoldenrodyellow" to "fafad2",
    "lightgray" to "d3d3d3",
    "lightgreen" to "90ee90",
    "lightgrey" to "d3d3d3",
    "lightpink" to "ffb6c1",
    "lightsalmon" to "ffa07a",
    "lightseagreen" to "20b2aa",
    "lightskyblue" to "87cefa",
    "lightslategray" to "778899",
    "lightslategrey" to "778899",
    "lightsteelblue" to "b0c4de",
    "lightyellow" to "ffffe0",
    "limegreen" to "32cd32",
    "linen" to "faf0e6",
    "mediumaquamarine" to "66cdaa",
    "mediumblue" to "0000cd",
    "mediumorchid" to "ba55d3",
    "mediumpurple" to "9370db",
    "mediumseagreen" to "3cb371",
    "mediumslateblue" to "7b68ee",
    "mediumspringgreen" to "00fa9a",
    "mediumturquoise" to "48d1cc",
    "mediumvioletred" to "c71585",
    "midnightblue" to "191970",
    "mintcream" to "f5fffa",
    "mistyrose" to "ffe4e1",
    "moccasin" to "ffe4b5",
    "navajowhite" to "ffdead",
    "oldlace" to "fdf5e6",
    "olivedrab" to "6b8e23",
    "orangered" to "ff4500",
    "orchid" to "da70d6",
    "palegoldenrod" to "eee8aa",
    "palegreen" to "98fb98",
    "paleturquoise" to "afeeee",
    "palevioletred" to "db7093",
    "papayawhip" to "ffefd5",
    "peachpuff" to "ffdab9",
    "peru" to "cd853f",
    "pink" to "ffc0cb",
    "plum" to "dda0dd",
    "powderblue" to "b0e0e6",
    "rosybrown" to "bc8f8f",
    "royalblue" to "4169e1",
    "saddlebrown" to "8b4513",
    "salmon" to "fa8072",
    "sandybrown" to "f4a460",
    "seagreen" to "2e8b57",
    "seashell" to "fff5ee",
    "sienna" to "a0522d",
    "skyblue" to "87ceeb",
    "slateblue" to "6a5acd",
    "slategray" to "708090",
    "slategrey" to "708090",
    "snow" to "fffafa",
    "springgreen" to "00ff7f",
    "steelblue" to "4682b4",
    "tan" to "d2b48c",
    "thistle" to "d8bfd8",
    "tomato" to "ff6347",
    "turquoise" to "40e0d0",
    "violet" to "ee82ee",
    "wheat" to "f5deb3",
    "whitesmoke" to "f5f5f5",
    "yellowgreen" to "9acd32",
    "rebeccapurple"	to "663399",
)

private fun NamedNodeMap.extractColor(key: String, default: Color): Color {
    val value = removeOrNull(key)?.nodeValue ?: return default

    if (value == "none") return Color(0x00000000u)

    val hex = if (value.startsWith("rgb")) {
        val (r, g, b) = value.removePrefix("rgb(")
            .trimEnd(')')
            .split(',')
            .map { it.trim().toShort() }

        "%02x%02x%02x".format(r, g, b)
    } else if (value.startsWith("#")) {
        val hex = value.trim('#')
        if (hex.length != 3) hex else ("${hex[0]}" + hex[0] + hex[1] + hex[1] + hex[2] + hex[2])
    } else {
        NAMED_COLORS[value] ?: return default
    }

    return Color(hex.toUInt(radix = 16) or 0xFF000000u)
}
