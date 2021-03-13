package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.*
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.svg.graphic.ClipPath
import com.jzbrooks.vgo.core.util.xml.asSequence
import com.jzbrooks.vgo.core.util.xml.toMutableMap
import org.w3c.dom.Comment
import org.w3c.dom.Node
import org.w3c.dom.Text

fun parse(root: Node): ScalableVectorGraphic {
    val rootMetadata = root.attributes.toMutableMap()

    val elements = root.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()

    return ScalableVectorGraphic(elements, rootMetadata)
}

private fun parseElement(node: Node): Element? {
    return if (node !is Text && node !is Comment) {
        when (node.nodeName) {
            "g" -> parseContainerElement(node, ::Group)
            "clipPath" -> parseContainerElement(node, ::ClipPath)
            "path" -> parsePathElement(node, ::Path)
            else -> parseExtraElement(node)
        }
    } else {
        null
    }
}

private fun <T : ContainerElement> parseContainerElement(containerElementNode: Node, generator: (List<Element>, MutableMap<String, String>) -> T): T {
    val childElements = containerElementNode.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val attributes = containerElementNode.attributes.toMutableMap()

    return generator(childElements, attributes)
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, MutableMap<String, String>) -> T): T {
    val metadata = node.attributes.asSequence()
            .filter { attribute -> attribute.nodeName != "d" }
            .associate { attribute -> attribute.nodeName to attribute.nodeValue }
            .toMutableMap()

    val data = CommandString(node.attributes.getNamedItem("d").textContent)
    return generator(data.toCommandList(), metadata)
}

private fun parseExtraElement(node: Node): Extra {
    val containedElements = node.childNodes.asSequence()
            .mapNotNull(::parseElement)
            .toList()
    val attributes = node.attributes?.toMutableMap() ?: mutableMapOf()

    return Extra(node.nodeValue ?: node.nodeName, containedElements, attributes)
}