package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.xml.asSequence
import com.jzbrooks.vgo.core.util.xml.removeOrNull
import com.jzbrooks.vgo.core.util.xml.toMutableMap
import com.jzbrooks.vgo.vd.graphic.ClipPath
import org.w3c.dom.Comment
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.Text
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val HEX_WITH_ALPHA = Regex("#[a-fA-F\\d]{8}")

fun parse(root: Node): VectorDrawable {
    val rootMetadata = root.attributes.toGraphicAttributes()

    val elements = root.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return VectorDrawable(elements, rootMetadata)
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

private fun parseGroup(groupNode: Node): Group {
    val groupChildElements = groupNode.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()
    val groupMetadata = groupNode.attributes.toGroupAttributes()
    return Group(groupChildElements, groupMetadata)
}

private fun parsePath(node: Node): Path {
    val pathDataString = node.attributes.getNamedItem("android:pathData")!!.textContent

    return if (pathDataString.startsWith('@') || pathDataString.startsWith('?')) {
        Path(emptyList(), node.attributes.toPathAttributes())
    } else {
        node.attributes.removeNamedItem("android:pathData")

        val data = CommandString(pathDataString)
        Path(data.toCommandList(), node.attributes.toPathAttributes())
    }
}

private fun parseClipPath(node: Node): ClipPath {
    val pathDataString = node.attributes.getNamedItem("android:pathData")!!.textContent

    return if (pathDataString.startsWith('@') || pathDataString.startsWith('?')) {
        ClipPath(emptyList(), node.attributes.toClipPathAttributes())
    } else {
        node.attributes.removeNamedItem("android:pathData")

        val data = CommandString(pathDataString)
        ClipPath(data.toCommandList(), node.attributes.toClipPathAttributes())
    }
}

private fun parseGroupElement(node: Node): Group {
    val attributes = node.attributes.toGroupAttributes()

    val childElements = node.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return Group(childElements, attributes)
}

private fun parsePathElement(node: Node): Path {
    val attributes = node.attributes.toPathAttributes()

    val data = CommandString(node.attributes.removeNamedItem("android:pathData").textContent)

    return Path(data.toCommandList(), attributes)
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
        .mapNotNull(::parseElement)
        .toList()

    return Extra(node.nodeValue ?: node.nodeName, containedElements, node.attributes.toExtraAttributes())
}

private fun NamedNodeMap.toGraphicAttributes() = VectorDrawable.Attributes(
    removeOrNull("android:name")?.nodeValue,
    toMutableMap(),
)

private fun NamedNodeMap.toPathAttributes(): Path.Attributes {
    return Path.Attributes(
        removeOrNull("android:name")?.nodeValue,
        toMutableMap(),
    )
}

private fun NamedNodeMap.toClipPathAttributes() = ClipPath.Attributes(
    removeOrNull("android:name")?.nodeValue,
    toMutableMap(),
)

private fun NamedNodeMap.toGroupAttributes() = Group.Attributes(
    removeOrNull("android:name")?.nodeValue,
    computeTransformationMatrix(),
    toMutableMap(),
)

private fun NamedNodeMap.toExtraAttributes() = Extra.Attributes(
    removeOrNull("android:name")?.nodeValue,
    toMutableMap(),
)

private fun NamedNodeMap.computeTransformationMatrix(): Matrix3 {
    val scaleX = removeFloatOrNull("android:scaleX")
    val scaleY = removeFloatOrNull("android:scaleY")

    val translationX = removeFloatOrNull("android:translateX")
    val translationY = removeFloatOrNull("android:translateY")

    val pivotX = removeFloatOrNull("android:pivotX")
    val pivotY = removeFloatOrNull("android:pivotY")

    val rotation = removeFloatOrNull("android:rotation")

    if (scaleX == null && scaleY == null &&
        translationX == null && translationY == null &&
        pivotX == null && pivotY == null && rotation == null
    ) return Matrix3.IDENTITY

    val scale = Matrix3.from(
        arrayOf(
            floatArrayOf(scaleX ?: 1f, 0f, 0f),
            floatArrayOf(0f, scaleY ?: 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
    )

    val translation = Matrix3.from(
        arrayOf(
            floatArrayOf(1f, 0f, translationX ?: 0f),
            floatArrayOf(0f, 1f, translationY ?: 0f),
            floatArrayOf(0f, 0f, 1f)
        )
    )

    val pivot = Matrix3.from(
        arrayOf(
            floatArrayOf(1f, 0f, pivotX ?: 0f),
            floatArrayOf(0f, 1f, pivotY ?: 0f),
            floatArrayOf(0f, 0f, 1f)
        )
    )

    val pivotInverse = Matrix3.from(
        arrayOf(
            floatArrayOf(1f, 0f, (pivotX ?: 0f) * -1),
            floatArrayOf(0f, 1f, (pivotY ?: 0f) * -1),
            floatArrayOf(0f, 0f, 1f)
        )
    )

    val rotate = if (rotation != null) {
        val radians = rotation * PI.toFloat() / 180f
        Matrix3.from(
            arrayOf(
                floatArrayOf(cos(radians), -sin(radians), 0f),
                floatArrayOf(sin(radians), cos(radians), 0f),
                floatArrayOf(0f, 0f, 1f)
            )
        )
    } else {
        Matrix3.IDENTITY
    }

    return listOf(pivot, translation, rotate, scale, pivotInverse).reduce(Matrix3::times)
}

private fun NamedNodeMap.removeFloatOrNull(key: String): Float? {
    val value = getNamedItem(key)?.nodeValue?.toFloatOrNull()

    if (value != null) {
        removeNamedItem(key)
    }

    return value
}
