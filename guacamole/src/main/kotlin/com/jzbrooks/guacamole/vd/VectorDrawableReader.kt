package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.Command
import com.jzbrooks.guacamole.core.graphic.command.CommandString
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.core.util.math.MutableMatrix3
import com.jzbrooks.guacamole.core.util.xml.asSequence
import com.jzbrooks.guacamole.core.util.xml.toMap
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun parse(input: InputStream): VectorDrawable {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
    document.documentElement.normalize()

    val root = document.childNodes.item(0)
    val rootMetadata = root.attributes.toMap()

    val elements = root.childNodes.asSequence()
            .map(::parseElement)
            .filterNotNull()
            .toList()

    return VectorDrawable(elements, rootMetadata)
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
    val groupChildElements = groupNode.childNodes.asSequence()
            .map(::parseElement)
            .filterNotNull()
            .toList()
    val groupMetadata = groupNode.attributes.toMap()

    val scaleX = groupMetadata["android:scaleX"]?.toFloat()
    val scaleY = groupMetadata["android:scaleY"]?.toFloat()

    val translationX = groupMetadata["android:translateX"]?.toFloat()
    val translationY = groupMetadata["android:translateY"]?.toFloat()

    val pivotX = groupMetadata["android:pivotX"]?.toFloat()
    val pivotY = groupMetadata["android:pivotY"]?.toFloat()

    val rotation = groupMetadata["android:rotation"]?.toFloat()

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

    return Group(groupChildElements, groupMetadata, nonIdentityTransform)
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, Map<String, String>) -> T): T {
    val metadata = node.attributes.asSequence()
            .filter { attribute -> attribute.nodeName != "android:pathData" }
            .associate { attribute -> attribute.nodeName to attribute.nodeValue }
            .toMap()

    val data = CommandString(node.attributes.getNamedItem("android:pathData").textContent)
    return generator(data.toCommandList(), metadata)
}