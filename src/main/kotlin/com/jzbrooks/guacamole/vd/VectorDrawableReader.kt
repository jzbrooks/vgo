package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.Command
import com.jzbrooks.guacamole.graphic.command.CommandString
import com.jzbrooks.guacamole.util.math.Point
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

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
        groupMetadata[attribute.nodeName] = attribute.nodeValue
    }

    return Group(groupChildElements, groupMetadata.toMap())
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, Map<String, String>) -> T): T {
    val metadata = mutableMapOf<String, String>()

    for (i in 0 until node.attributes.length) {
        val attribute = node.attributes.item(i)
        if (attribute.nodeName != "android:pathData") {
            metadata[attribute.nodeName] = attribute.nodeValue
        }
    }
    val data = CommandString(node.attributes.getNamedItem("android:pathData").textContent)
    return generator(data.toCommandList(), metadata.toMap())
}
