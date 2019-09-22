package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.graphic.*
import com.jzbrooks.guacamole.graphic.command.Command
import com.jzbrooks.guacamole.graphic.command.CommandString
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

fun parse(input: InputStream): VectorDrawable {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
    document.documentElement.normalize()

    val widthText = document.documentElement.attributes.getNamedItem("android:width").textContent
    val width = widthText.removeSuffix("dp").toInt()
    val widthDimension = if (widthText.endsWith("dp")) Dimension.Unit.Dp else Dimension.Unit.Px
    val heightText = document.documentElement.attributes.getNamedItem("android:height").textContent
    val height = heightText.removeSuffix("dp").toInt()
    val heightDimension = if (heightText.endsWith("dp")) Dimension.Unit.Dp else Dimension.Unit.Px

    val elements = mutableListOf<Element>()
    val rootMetadata = mutableMapOf<String, String>()
    val root = document.childNodes.item(0)

    root.attributes.getNamedItem("android:name")?.let { node ->
        rootMetadata[node.nodeName] = node.nodeValue
    }

    for (index in 0 until root.childNodes.length) {
        val element = root.childNodes.item(index)
        if (element !is Text) {
            when (element.nodeName) {
                "group" -> elements.add(parseGroup(element))
                "path" -> elements.add(parsePathElement(element, ::Path))
                "clip-path" -> elements.add(parsePathElement(element, ::ClipPath))
                else -> System.err.println("Unknown document element: ${element.nodeName}")
            }
        }
    }

    return VectorDrawable(elements, Size(Dimension(width, widthDimension), Dimension(height, heightDimension)), rootMetadata.toMap())
}

private fun parseGroup(groupNode: Node): Group {
    val groupChildElements = mutableListOf<Element>()
    val groupMetadata = mutableMapOf<String, String>()

    for (index in 0 until groupNode.childNodes.length) {
        val child = groupNode.childNodes.item(index)
        if (child !is Text) {
            when (child.nodeName) {
                "group" -> groupChildElements.add(parseGroup(child))
                "path" -> groupChildElements.add(parsePathElement(child, ::Path))
                "clip-path" -> groupChildElements.add(parsePathElement(child, ::ClipPath))
                else -> System.err.println("Unknown document element: ${child.nodeName}")
            }
        }
    }

    return Group(groupChildElements, groupMetadata.toMap())
}

private fun <T : PathElement> parsePathElement(node: Node, generator: (List<Command>, Map<String, String>) -> T): T {
    val metadata = mutableMapOf<String, String>()

    if (node.attributes != null) {
        for (i in 0 until node.attributes.length) {
            val attribute = node.attributes.item(i)
            if (attribute.nodeName != "android:pathData") {
                metadata[attribute.nodeName] = attribute.nodeValue
            }
        }
    }

    val data = CommandString(node.attributes.getNamedItem("android:pathData").textContent)
    return generator(data.toCommandList(), metadata.toMap())
}
