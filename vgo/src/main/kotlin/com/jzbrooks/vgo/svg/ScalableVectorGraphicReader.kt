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
import com.jzbrooks.vgo.util.xml.extractLineCap
import com.jzbrooks.vgo.util.xml.extractLineJoin
import com.jzbrooks.vgo.util.xml.removeFloatOrNull
import com.jzbrooks.vgo.util.xml.removeOrNull
import com.jzbrooks.vgo.util.xml.toMutableMap
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
    val fillRule = node.attributes.extractFillRule("fill-rule")
    val stroke = node.attributes.extractColor("stroke", Colors.TRANSPARENT)
    val strokeWidth = node.attributes.removeFloatOrNull("stroke-width") ?: 1f
    val strokeLineCap = node.attributes.extractLineCap("stroke-linecap")
    val strokeLineJoin = node.attributes.extractLineJoin("stroke-linejoin")
    val strokeMiterLimit = node.attributes.removeFloatOrNull("stroke-miterlimit") ?: 4f

    return Path(
        commands,
        id,
        node.attributes.toMutableMap(),
        fill,
        fillRule,
        stroke,
        strokeWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeMiterLimit,
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
        floatArrayOf(entries[0], entries[2], entries[4], entries[1], entries[3], entries[5], 0f, 0f, 1f)
    )
}

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
        return Colors.COLORS_BY_NAMES[value] ?: default
    }

    return Color(hex.toUInt(radix = 16) or 0xFF000000u)
}

private fun NamedNodeMap.extractFillRule(key: String) = when (removeOrNull(key)?.nodeValue) {
    "evenodd" -> Path.FillRule.EVEN_ODD
    else -> Path.FillRule.NON_ZERO
}
