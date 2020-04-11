package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.util.xml.asSequence
import com.jzbrooks.vgo.util.xml.toMutableMap
import com.jzbrooks.vgo.vd.graphic.ClipPath
import org.w3c.dom.Comment
import org.w3c.dom.Node
import org.w3c.dom.Text

fun parse(root: Node): VectorDrawable {
    val rootMetadata = root.attributes.toMutableMap()

    val elements = root.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return VectorDrawable(elements, rootMetadata)
}

private fun parseElement(node: Node): Element? {
    return if (node !is Text && node !is Comment) {
        when (node.nodeName) {
            "group" -> parseGroup(node)
            "path" -> parsePathElement(node, ::Path)
            "clip-path" -> parsePathElement(node, ::ClipPath)
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
    val groupMetadata = groupNode.attributes.toMutableMap()
    return Group(groupChildElements, groupMetadata)
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, MutableMap<String, String>) -> T): T {
    val metadata = node.attributes.asSequence()
            .filter { attribute -> attribute.nodeName != "android:pathData" }
            .associate { attribute -> attribute.nodeName to attribute.nodeValue }
            .toMutableMap()

    val pathDataString = node.attributes.getNamedItem("android:pathData").textContent

    return if (pathDataString.startsWith('@') || pathDataString.startsWith('?')) {
        metadata["android:pathData"] = pathDataString
        generator(emptyList(), metadata)
    } else {
        val data = CommandString(pathDataString)
        generator(data.toCommandList(), metadata)
    }
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val attributes = node.attributes?.toMutableMap() ?: mutableMapOf()

    return Extra(node.nodeValue ?: node.nodeName, containedElements, attributes)
}