package com.jzbrooks.guacamole.svg

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.Command
import com.jzbrooks.guacamole.core.graphic.command.CommandString
import com.jzbrooks.guacamole.core.util.math.Matrix3
import com.jzbrooks.guacamole.core.util.math.MutableMatrix3
import com.jzbrooks.guacamole.core.util.xml.asSequence
import com.jzbrooks.guacamole.core.util.xml.toMap
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun parse(document: Document): ScalableVectorGraphic {
    val root = document.childNodes.item(0)
    val rootMetadata = root.attributes.toMap()

    val elements = root.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return ScalableVectorGraphic(elements, rootMetadata)
}

private fun parseElement(node: Node): Element? {
    return if (node !is Text) {
        when (node.nodeName) {
            "g" -> parseGroup(node)
            "path" -> parsePathElement(node, ::Path)
            "clipPath" -> parsePathElement(node, ::ClipPath)
            else -> parseExtraElement(node)
        }
    } else {
        null
    }
}

private fun parseGroup(groupNode: Node): Group {
    val groupChildElements = groupNode.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val groupMetadata = groupNode.attributes.toMap()

//    val scaleX = groupMetadata["android:scaleX"]?.toFloat()
//    val scaleY = groupMetadata["android:scaleY"]?.toFloat()
//
//    val translationX = groupMetadata["android:translateX"]?.toFloat()
//    val translationY = groupMetadata["android:translateY"]?.toFloat()
//
//    val pivotX = groupMetadata["android:pivotX"]?.toFloat()
//    val pivotY = groupMetadata["android:pivotY"]?.toFloat()
//
//    val rotation = groupMetadata["android:rotation"]?.toFloat()
//
//    val scale : Matrix3 = MutableMatrix3().apply {
//        this[0, 0] = scaleX ?: 1f
//        this[1, 1] = scaleY ?: 1f
//    }
//
//    val translation: Matrix3 = MutableMatrix3().apply {
//        this[0, 2] = translationX ?: 0f
//        this[1, 2] = translationY ?: 0f
//    }
//
//    val pivot: Matrix3 = MutableMatrix3().apply {
//        this[0, 2] = pivotX ?: 0f
//        this[1, 2] = pivotY ?: 0f
//    }
//
//    val antiPivot: Matrix3 = MutableMatrix3().apply {
//        this[0, 2] = (pivotX ?: 0f) * -1
//        this[1, 2] = (pivotY ?: 0f) * -1
//    }
//
//    val rotate: Matrix3 = MutableMatrix3().apply {
//        rotation?.let {
//            val radians = it * PI.toFloat() / 180f
//            this[0, 0] = cos(radians)
//            this[0, 1] = -sin(radians)
//            this[1, 0] = sin(radians)
//            this[1, 1] = cos(radians)
//        }
//    }
//
//    val transform = listOf(pivot, rotate, translation, scale, antiPivot).reduce(Matrix3::times)
//    val nonIdentityTransform = if (transform == Matrix3.IDENTITY) null else transform

    return Group(groupChildElements, groupMetadata, null)
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, Map<String, String>) -> T): T {
    val metadata = node.attributes.asSequence()
            .filter { attribute -> attribute.nodeName != "d" }
            .associate { attribute -> attribute.nodeName to attribute.nodeValue }
            .toMap()

    val data = CommandString(node.attributes.getNamedItem("d").textContent)
    return generator(data.toCommandList(), metadata)
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val attributes = node.attributes?.toMap() ?: emptyMap()

    return Extra(node.nodeValue ?: node.nodeName, containedElements, attributes)
}