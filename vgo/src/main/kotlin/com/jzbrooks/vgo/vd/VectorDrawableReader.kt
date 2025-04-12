package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.computeTransformation
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
import kotlin.math.roundToInt

fun parse(root: Node): VectorDrawable {
    val elements =
        root.childNodes
            .asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return VectorDrawable(
        elements,
        root.attributes.removeOrNull("android:name")?.nodeValue,
        root.attributes.toMutableMap(),
    )
}

private fun parseElement(node: Node): Element? {
    if (node is Text || node is Comment) return null

    return when (node.nodeName) {
        "group" -> parseGroup(node)
        "path" -> parsePath(node)
        "clip-path" -> parseClipPath(node)
        else -> parseExtraElement(node)
    }
}

private fun parseGroup(node: Node): Group {
    val groupChildElements =
        node.childNodes
            .asSequence()
            .mapNotNull(::parseElement)
            .toList()

    // This has to happen before foreign property collection
    val transform = node.attributes.computeTransformationMatrix()
    return Group(
        groupChildElements,
        node.attributes.removeOrNull("android:name")?.nodeValue,
        node.attributes.toMutableMap(),
        transform,
    )
}

private fun parsePath(node: Node): Path {
    val pathDataString = node.attributes.getNamedItem("android:pathData")!!.textContent

    val id = node.attributes.removeOrNull("android:name")?.nodeValue
    val fill = node.attributes.extractColor("android:fillColor", "android:fillAlpha", Colors.TRANSPARENT)
    val fillRule = node.attributes.extractFillRule("android:fillType")
    val stroke = node.attributes.extractColor("android:strokeColor", "android:strokeAlpha", Colors.TRANSPARENT)
    val strokeWidth = node.attributes.removeFloatOrNull("android:strokeWidth") ?: 0f
    val strokeLineCap = node.attributes.extractLineCap("android:strokeLineCap")
    val strokeLineJoin = node.attributes.extractLineJoin("android:strokeLineJoin")
    val strokeMiterLimit = node.attributes.removeFloatOrNull("android:strokeMiterLimit") ?: 4f

    return if (pathDataString.startsWith('@') || pathDataString.startsWith('?')) {
        Path(
            id,
            node.attributes.toMutableMap(),
            emptyList(),
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
        )
    } else {
        node.attributes.removeNamedItem("android:pathData")

        Path(
            id,
            node.attributes.toMutableMap(),
            CommandString(pathDataString).toCommandList(),
            fill,
            fillRule,
            stroke,
            strokeWidth,
            strokeLineCap,
            strokeLineJoin,
            strokeMiterLimit,
        )
    }
}

private fun parseClipPath(node: Node): ClipPath {
    val pathDataString = node.attributes.getNamedItem("android:pathData")!!.textContent

    return if (pathDataString.startsWith('@') || pathDataString.startsWith('?')) {
        ClipPath(
            listOf(
                Path(
                    null,
                    mutableMapOf(),
                    emptyList(),
                    Colors.TRANSPARENT,
                    Path.FillRule.NON_ZERO,
                    Colors.TRANSPARENT,
                    0f,
                    Path.LineCap.BUTT,
                    Path.LineJoin.MITER,
                    4f,
                ),
            ),
            node.attributes.removeOrNull("android:name")?.nodeValue,
            node.attributes.toMutableMap(),
        )
    } else {
        node.attributes.removeNamedItem("android:pathData")

        ClipPath(
            listOf(
                Path(
                    null,
                    mutableMapOf(),
                    CommandString(pathDataString).toCommandList(),
                    Colors.TRANSPARENT,
                    Path.FillRule.NON_ZERO,
                    Colors.TRANSPARENT,
                    0f,
                    Path.LineCap.BUTT,
                    Path.LineJoin.MITER,
                    4f,
                ),
            ),
            node.attributes.removeOrNull("android:name")?.nodeValue,
            node.attributes.toMutableMap(),
        )
    }
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements =
        node.childNodes
            .asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return Extra(
        node.nodeValue ?: node.nodeName,
        containedElements,
        node.attributes.removeOrNull("android:name")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun NamedNodeMap.computeTransformationMatrix(): Matrix3 {
    val scaleX = removeFloatOrNull("android:scaleX")
    val scaleY = removeFloatOrNull("android:scaleY")

    val translationX = removeFloatOrNull("android:translateX")
    val translationY = removeFloatOrNull("android:translateY")

    val pivotX = removeFloatOrNull("android:pivotX")
    val pivotY = removeFloatOrNull("android:pivotY")

    val rotation = removeFloatOrNull("android:rotation")

    if (scaleX == null &&
        scaleY == null &&
        translationX == null &&
        translationY == null &&
        pivotX == null &&
        pivotY == null &&
        rotation == null
    ) {
        return Matrix3.IDENTITY
    }

    return computeTransformation(scaleX, scaleY, translationX, translationY, rotation, pivotX, pivotY)
}

private fun NamedNodeMap.extractColor(
    key: String,
    alphaKey: String,
    default: Color,
): Color {
    // This will be overwritten at the end of path writing as a foreign property
    if (getNamedItem(key)?.nodeValue?.startsWith('@') == true ||
        getNamedItem(alphaKey)?.nodeValue?.startsWith('@') == true ||
        getNamedItem(key)?.nodeValue?.startsWith('?') == true ||
        getNamedItem(alphaKey)?.nodeValue?.startsWith('?') == true
    ) {
        return default
    }

    val value = removeOrNull(key)?.nodeValue ?: return default

    val alpha =
        removeFloatOrNull(alphaKey)?.let { alpha ->
            (alpha * 255).roundToInt().toUInt()
        }

    var colorInt =
        when (value.length) {
            9 -> value.trimStart('#').toUInt(radix = 16)
            4 -> ("${value[1]}" + value[1] + value[2] + value[2] + value[3] + value[3]).toUInt(radix = 16) or 0xFF000000u
            else -> value.trimStart('#').toUInt(radix = 16) or 0xFF000000u
        }

    // Alpha is determined by min(color MSB, alpha attribute)
    if (alpha != null && alpha < (colorInt shr 24)) {
        colorInt = colorInt and 0x00FFFFFFu or (alpha shl 24)
    }

    return Color(colorInt)
}

fun NamedNodeMap.extractFillRule(key: String) =
    when (removeOrNull(key)?.nodeValue) {
        "evenOdd" -> Path.FillRule.EVEN_ODD
        else -> Path.FillRule.NON_ZERO
    }
