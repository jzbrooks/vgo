package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.util.xml.asSequence
import com.jzbrooks.vgo.util.xml.removeOrNull
import com.jzbrooks.vgo.util.xml.toMutableMap
import org.w3c.dom.Comment
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.Text

private val PRESENTATION_ATTRIBUTES: Set<String> =
    hashSetOf("fill", "fill-rule", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit")

fun parse(root: Node): ScalableVectorGraphic {
    val rootStyleProperties =
        root.attributes
            .getNamedItem("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()
    val rootAttrs = root.attributes.asSequence().associate { it.nodeName to it.nodeValue }

    // Style properties should overwrite inherited attributes
    val inherited = rootAttrs + rootStyleProperties

    val elements =
        root.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, inherited) }
            .toList()

    return ScalableVectorGraphic(
        elements,
        root.attributes.removeOrNull("id")?.nodeValue,
        root.attributes.toMutableMap(),
    )
}

private fun parseElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Element? {
    if (node is Text || node is Comment) return null

    return when (node.nodeName) {
        "g" -> parseGroupElement(node, inherited)
        "clipPath" -> parseClipPath(node, inherited)
        "path" -> parsePathElement(node, inherited)
        else -> parseExtraElement(node, inherited)
    }
}

private fun parseClipPath(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): ClipPath {
    val childInherited = collectStyleInheritance(node, inherited)

    val childElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
            .toList()

    node.attributes.removeOrNull("style")

    return ClipPath(
        childElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun parseGroupElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Group {
    val childInherited = collectStyleInheritance(node, inherited)

    val childElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
            .toList()

    node.attributes.removeOrNull("style")

    // This has to happen before foreign property collection
    val transform = node.attributes.extractTransformMatrix()
    return Group(
        childElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
        transform,
    )
}

/**
 * Builds the inherited presentation attribute context for children of [node].
 * Precedence: ancestor inherited < node's own presentation attributes < node's style attribute
 */
private fun collectStyleInheritance(
    node: Node,
    inherited: Map<String, String>,
): Map<String, String> {
    val styleProperties =
        node.attributes
            .getNamedItem("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()
    val nodeAttrs = node.attributes.asSequence().associate { it.nodeName to it.nodeValue }

    // The order of concatenation is important for precedence.
    return inherited + nodeAttrs + styleProperties
}

private fun parsePathElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Path {
    val styleProperties =
        node.attributes
            .removeOrNull("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()

    val commands =
        CommandString(
            node.attributes
                .removeNamedItem("d")
                .nodeValue
                .toString(),
        ).toCommandList()
    val id = node.attributes.removeOrNull("id")?.nodeValue

    // Snapshot presentation attrs before removing them so they don't appear in foreign
    val nodeAttrMap =
        node.attributes
            .asSequence()
            .filter { it.nodeName in PRESENTATION_ATTRIBUTES }
            .associate { it.nodeName to it.nodeValue }

    for (attr in PRESENTATION_ATTRIBUTES) {
        node.attributes.removeOrNull(attr)
    }

    // The order of concatenation is important for precedence.
    // style > node attrs > inherited > CSS initial value
    val merged = inherited + nodeAttrMap + styleProperties

    val fill = merged.extractColor("fill", Colors.BLACK) ?: Colors.BLACK
    val fillRule = merged.extractFillRule("fill-rule") ?: Path.FillRule.NON_ZERO
    val stroke = merged.extractColor("stroke", Colors.TRANSPARENT) ?: Colors.TRANSPARENT
    val strokeWidth = merged["stroke-width"]?.toFloatOrNull() ?: 1f
    val strokeLineCap = merged.extractLineCap("stroke-linecap") ?: Path.LineCap.BUTT
    val strokeLineJoin = merged.extractLineJoin("stroke-linejoin") ?: Path.LineJoin.MITER
    val strokeMiterLimit = merged["stroke-miterlimit"]?.toFloatOrNull() ?: 4f

    return Path(
        id,
        node.attributes.toMutableMap(),
        commands,
        fill,
        fillRule,
        stroke,
        strokeWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeMiterLimit,
    )
}

private fun parseExtraElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Extra {
    val childInherited = collectStyleInheritance(node, inherited)

    val containedElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
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

    val entries =
        transform
            .removePrefix("matrix(")
            .trimEnd(')')
            .split(',')
            .map(String::toFloat)

    return Matrix3.from(
        floatArrayOf(entries[0], entries[2], entries[4], entries[1], entries[3], entries[5], 0f, 0f, 1f),
    )
}

private fun parseColorValue(
    value: String,
    default: Color,
): Color {
    if (value == "none") return Color(0x00000000u)

    val hex =
        if (value.startsWith("rgb")) {
            val (r, g, b) =
                value
                    .removePrefix("rgb(")
                    .trimEnd(')')
                    .split(',')
                    .map { it.trim().toShort() }

            "%02x%02x%02x".format(r, g, b)
        } else if (value.startsWith("#")) {
            val hex = value.trim('#')
            if (hex.length != 3) hex else ("${hex[0]}" + hex[0] + hex[1] + hex[1] + hex[2] + hex[2])
        } else {
            return Colors.COLORS_BY_NAMES[value] ?: default
        }

    return Color(hex.toUInt(radix = 16) or 0xFF000000u)
}

private fun Map<String, String>.extractColor(
    key: String,
    default: Color,
): Color? {
    val value = this[key] ?: return null
    return parseColorValue(value, default)
}

private fun Map<String, String>.extractFillRule(key: String): Path.FillRule? =
    when (this[key]) {
        "evenodd" -> Path.FillRule.EVEN_ODD
        "nonzero" -> Path.FillRule.NON_ZERO
        else -> null
    }

private fun Map<String, String>.extractLineCap(key: String): Path.LineCap? =
    when (this[key]) {
        "round" -> Path.LineCap.ROUND
        "square" -> Path.LineCap.SQUARE
        "butt" -> Path.LineCap.BUTT
        else -> null
    }

private fun Map<String, String>.extractLineJoin(key: String): Path.LineJoin? =
    when (this[key]) {
        "round" -> Path.LineJoin.ROUND
        "bevel" -> Path.LineJoin.BEVEL
        "arcs" -> Path.LineJoin.ARCS
        "miter-clip" -> Path.LineJoin.MITER_CLIP
        "miter" -> Path.LineJoin.MITER
        else -> null
    }

private fun String.parseStyleAttribute(): Map<String, String> =
    split(';')
        .mapNotNull { property ->
            val colonIndex = property.indexOf(':')
            if (colonIndex > 0) {
                property.substring(0, colonIndex).trim() to property.substring(colonIndex + 1).trim()
            } else {
                null
            }
        }.toMap()
