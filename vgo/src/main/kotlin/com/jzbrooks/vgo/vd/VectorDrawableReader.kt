package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import com.jzbrooks.vgo.core.TileMode
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
    val elements = partitionChildren(root.childNodes.asSequence())

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
        else -> parseExtraElement(node)
    }
}

// VD semantics: a <clip-path> only affects siblings that appear *after* it in
// source order. Translate that positional form into the IR's group-scoped form
// by synthesizing nested Groups whose clipPaths cover only the elements that
// followed each clip-path. The synthetic group is always the last entry in its
// parent's elements list, which the writer relies on to elide it on emit.
private fun partitionChildren(rawChildren: Sequence<Node>): List<Element> {
    val rootScope = mutableListOf<Element>()
    var currentScope: MutableList<Element> = rootScope
    val pendingClipPaths = mutableListOf<ClipPath>()

    for (child in rawChildren) {
        if (child is Text || child is Comment) continue
        if (child.nodeName == "clip-path") {
            pendingClipPaths += parseClipPath(child)
            continue
        }
        val parsed = parseElement(child) ?: continue

        if (pendingClipPaths.isNotEmpty()) {
            val syntheticElements = mutableListOf<Element>()
            val synthetic =
                Group(
                    elements = syntheticElements,
                    clipPaths = pendingClipPaths.toList(),
                )
            currentScope.add(synthetic)
            currentScope = syntheticElements
            pendingClipPaths.clear()
        }
        currentScope.add(parsed)
    }

    return rootScope
}

private fun parseGroup(node: Node): Group {
    val groupChildElements = partitionChildren(node.childNodes.asSequence())

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
    val fill: Brush =
        node.attributes.extractColor("android:fillColor", "android:fillAlpha")
            ?: node.parseAaptGradient("android:fillColor")
            ?: Colors.TRANSPARENT

    val fillRule = node.attributes.extractFillRule("android:fillType")

    val stroke: Brush =
        node.attributes.extractColor("android:strokeColor", "android:strokeAlpha")
            ?: node.parseAaptGradient("android:strokeColor")
            ?: Colors.TRANSPARENT

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

    // Resource references (@drawable/x, ?attr/x) can't be resolved into commands;
    // preserve the original android:pathData in foreign and emit an empty Path so
    // downstream transforms know there's nothing optimizable.
    val isResourceReference = pathDataString.startsWith('@') || pathDataString.startsWith('?')
    val commands = if (isResourceReference) emptyList() else CommandString(pathDataString).toCommandList()

    if (!isResourceReference) {
        node.attributes.removeNamedItem("android:pathData")
    }

    return ClipPath(
        listOf(
            Path(
                id = null,
                foreign = mutableMapOf(),
                commands = commands,
                fill = Colors.TRANSPARENT,
                fillRule = Path.FillRule.NON_ZERO,
                stroke = Colors.TRANSPARENT,
                strokeWidth = 0f,
                strokeLineCap = Path.LineCap.BUTT,
                strokeLineJoin = Path.LineJoin.MITER,
                strokeMiterLimit = 4f,
            ),
        ),
        node.attributes.removeOrNull("android:name")?.nodeValue,
        node.attributes.toMutableMap(),
    )
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
): Color? {
    // This will be overwritten at the end of path writing as a foreign property
    if (getNamedItem(key)?.nodeValue?.startsWith('@') == true ||
        getNamedItem(alphaKey)?.nodeValue?.startsWith('@') == true ||
        getNamedItem(key)?.nodeValue?.startsWith('?') == true ||
        getNamedItem(alphaKey)?.nodeValue?.startsWith('?') == true
    ) {
        return null
    }

    val value = removeOrNull(key)?.nodeValue ?: return null

    val alpha =
        removeFloatOrNull(alphaKey)?.let { alpha ->
            (alpha * 255).roundToInt().toUInt()
        }

    var colorInt = parseColorInt(value)

    // Alpha is determined by min(color MSB, alpha attribute)
    if (alpha != null && alpha < (colorInt shr 24)) {
        colorInt = colorInt and 0x00FFFFFFu or (alpha shl 24)
    }

    return Color(colorInt)
}

private fun parseColorInt(value: String): UInt =
    when (value.length) {
        9 -> value.trimStart('#').toUInt(radix = 16)
        4 -> ("${value[1]}" + value[1] + value[2] + value[2] + value[3] + value[3]).toUInt(radix = 16) or 0xFF000000u
        else -> value.trimStart('#').toUInt(radix = 16) or 0xFF000000u
    }

private fun Node.parseAaptGradient(attrName: String): Brush? {
    val aaptAttr =
        childNodes
            .asSequence()
            .filterIsInstance<org.w3c.dom.Element>()
            .firstOrNull { it.nodeName == "aapt:attr" && it.getAttribute("name") == attrName }
            ?: return null

    val gradientNode =
        aaptAttr
            .childNodes
            .asSequence()
            .filterIsInstance<org.w3c.dom.Element>()
            .firstOrNull { it.nodeName == "gradient" } ?: return null

    val stops =
        gradientNode
            .childNodes
            .asSequence()
            .filterIsInstance<org.w3c.dom.Element>()
            .filter { it.nodeName == "item" }
            .map { item ->
                val offset = item.getAttribute("android:offset").toFloatOrNull() ?: 0f
                val color = Color(parseColorInt(item.getAttribute("android:color")))
                GradientStop(offset, color)
            }.toList()

    val tileMode =
        when (gradientNode.getAttribute("android:tileMode")) {
            "repeat" -> TileMode.REPEAT
            "mirror" -> TileMode.MIRROR
            else -> TileMode.CLAMP
        }

    // Remove the consumed aapt:attr so it doesn't leak into a downstream serializer.
    removeChild(aaptAttr)

    return when (gradientNode.getAttribute("android:type").ifEmpty { "linear" }) {
        "radial" -> {
            RadialGradient(
                centerX = gradientNode.getAttribute("android:centerX").toFloat(),
                centerY = gradientNode.getAttribute("android:centerY").toFloat(),
                radius = gradientNode.getAttribute("android:gradientRadius").toFloat(),
                stops = stops,
                tileMode = tileMode,
            )
        }

        "sweep" -> {
            SweepGradient(
                centerX = gradientNode.getAttribute("android:centerX").toFloat(),
                centerY = gradientNode.getAttribute("android:centerY").toFloat(),
                stops = stops,
            )
        }

        else -> {
            LinearGradient(
                startX = gradientNode.getAttribute("android:startX").toFloat(),
                startY = gradientNode.getAttribute("android:startY").toFloat(),
                endX = gradientNode.getAttribute("android:endX").toFloat(),
                endY = gradientNode.getAttribute("android:endY").toFloat(),
                stops = stops,
                tileMode = tileMode,
            )
        }
    }
}

fun NamedNodeMap.extractFillRule(key: String) =
    when (removeOrNull(key)?.nodeValue) {
        "evenOdd" -> Path.FillRule.EVEN_ODD
        else -> Path.FillRule.NON_ZERO
    }
