package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.Command
import com.jzbrooks.guacamole.graphic.command.CommandString
import com.jzbrooks.guacamole.util.math.Matrix3
import com.jzbrooks.guacamole.util.math.MutableMatrix3
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val internallyRepresentedGroupAttributes = setOf(
        "android:scaleX",
        "android:scaleY",
        "android:translateX",
        "android:translateY",
        "android:pivotX",
        "android:pivotY",
        "android:rotation"
)

private val internallyRepresentedPathElementAttributes = setOf(
        "android:pathData"
)

fun parse(input: InputStream): VectorDrawable {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
    document.documentElement.normalize()

    val elements = mutableListOf<Element>()
    val rootMetadata = mutableMapOf<String, String>()
    val root = document.childNodes.item(0)

    for (i in 0 until root.attributes.length) {
        val node = root.attributes.item(i)
        rootMetadata[node.nodeName] = node.nodeValue
    }

    for (index in 0 until root.childNodes.length) {
        val node = root.childNodes.item(index)
        val element = parseElement(node)
        if (element != null) {
            elements.add(element)
        }
    }

    return VectorDrawable(elements, rootMetadata.toMap())
}

private fun parseElement(node: Node): Element? {
    return if (node !is Text) {
        when (node.nodeName) {
            "group" -> parseGroup(node)
            "path" -> parsePathElement(node, ::Path)
            "clip-path" -> parsePathElement(node, ::ClipPath)
            else -> { System.err.println("Skipping unknown document element: ${node.nodeName}"); null }
        }
    } else {
        null
    }
}

private fun parseGroup(groupNode: Node): Group {
    val groupChildElements = mutableListOf<Element>()
    val groupMetadata = mutableMapOf<String, String>()

    for (index in 0 until groupNode.childNodes.length) {
        val child = groupNode.childNodes.item(index)
        val element = parseElement(child)
        if (element != null) {
            groupChildElements.add(element)
        }
    }

    for (index in 0 until groupNode.attributes.length) {
        val attribute = groupNode.attributes.item(index)
        if (!internallyRepresentedGroupAttributes.contains(attribute.nodeName)) {
            groupMetadata[attribute.nodeName] = attribute.nodeValue
        }
    }

    val scaleX = groupNode.attributes.getNamedItem("android:scaleX")?.nodeValue?.toFloat()
    val scaleY = groupNode.attributes.getNamedItem("android:scaleY")?.nodeValue?.toFloat()

    val translationX = groupNode.attributes.getNamedItem("android:translateX")?.nodeValue?.toFloat()
    val translationY = groupNode.attributes.getNamedItem("android:translateY")?.nodeValue?.toFloat()

    val pivotX = groupNode.attributes.getNamedItem("android:pivotX")?.nodeValue?.toFloat()
    val pivotY = groupNode.attributes.getNamedItem("android:pivotY")?.nodeValue?.toFloat()

    val rotation = groupNode.attributes.getNamedItem("android:rotation")?.nodeValue?.toFloat()

    val scale : Matrix3 = MutableMatrix3().apply {
        this[0, 0] = scaleX ?: 1f
        this[1, 1] = scaleY ?: 1f
    }

    val translation: Matrix3 = MutableMatrix3().apply {
        this[0, 2] = translationX ?: 0f
        this[1, 2] = translationY ?: 0f
    }

    val pivot: Matrix3 = MutableMatrix3().apply {
        this[0, 2] = pivotX ?: 0f
        this[1, 2] = pivotY ?: 0f
    }

    val antiPivot: Matrix3 = MutableMatrix3().apply {
        this[0, 2] = (pivotX ?: 0f) * -1
        this[1, 2] = (pivotY ?: 0f) * -1
    }

    val rotate: Matrix3 = MutableMatrix3().apply {
        rotation?.let {
            val radians = it * PI.toFloat() / 180f
            this[0, 0] = cos(radians)
            this[0, 1] = -sin(radians)
            this[1, 0] = sin(radians)
            this[1, 1] = cos(radians)
        }
    }

    val transform = listOf(pivot, rotate, translation, scale, antiPivot).reduce(Matrix3::times)
    val nonIdentityTransform = if (transform == Matrix3.IDENTITY) null else transform

    return Group(groupChildElements, groupMetadata.toMap(), nonIdentityTransform)
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, Map<String, String>) -> T): T {
    val metadata = mutableMapOf<String, String>()

    for (i in 0 until node.attributes.length) {
        val attribute = node.attributes.item(i)
        if (!internallyRepresentedPathElementAttributes.contains(attribute.nodeName)) {
            metadata[attribute.nodeName] = attribute.nodeValue
        }
    }
    val data = CommandString(node.attributes.getNamedItem("android:pathData").textContent)
    return generator(data.toCommandList(), metadata.toMap())
}
