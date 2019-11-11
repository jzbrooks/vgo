package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.core.graphic.*
import com.jzbrooks.guacamole.core.graphic.command.Command
import com.jzbrooks.guacamole.core.graphic.command.CommandString
import com.jzbrooks.guacamole.util.xml.asSequence
import com.jzbrooks.guacamole.util.xml.toMutableMap
import com.jzbrooks.guacamole.vd.graphic.ClipPath
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.Text

fun parse(document: Document): VectorDrawable {

    val root = document.firstChild
    val rootMetadata = root.attributes.toMutableMap()

    val elements = root.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return VectorDrawable(elements, rootMetadata)
}

private fun parseElement(node: Node): Element? {
    return if (node !is Text) {
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

    val data = CommandString(node.attributes.getNamedItem("android:pathData").textContent)
    return generator(data.toCommandList(), metadata)
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val attributes = node.attributes?.toMutableMap() ?: mutableMapOf()

    return Extra(node.nodeValue ?: node.nodeName, containedElements, attributes)
}