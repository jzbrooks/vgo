package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.*
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
    val root = document.childNodes.item(0)
    for (index in 0 until root.childNodes.length) {
        val element = root.childNodes.item(index)
        if (element !is Text) {
            when (element.nodeName) {
                "group" -> elements.add(parseGroup(element))
                "path" -> elements.add(parsePath(element))
                "clip-path" -> elements.add(parseClipPath(element))
                else -> System.err.println("Unknown document element: ${element.nodeName}")
            }
        }
    }

    return VectorDrawable(elements, Size(Dimension(width, widthDimension), Dimension(height, heightDimension)))
}

private fun parseGroup(groupNode: Node): Group {
    val groupPathList = mutableListOf<Path>()

    for (child in 0 until groupNode.childNodes.length) {
        val childPath = groupNode.childNodes.item(child)
        val data = childPath.attributes.getNamedItem("android:pathData").textContent
        val strokeWidth = childPath.attributes.getNamedItem("android:strokeWidth").textContent.toInt()
        groupPathList.add(Path(data, strokeWidth))
    }

    return Group(groupPathList)
}

private fun parsePath(pathNode: Node): Path {
    val data = pathNode.attributes.getNamedItem("android:pathData").textContent
    val strokeWidth = pathNode.attributes.getNamedItem("android:strokeWidth").textContent.toInt()
    return Path(data, strokeWidth)
}

private fun parseClipPath(pathNode: Node): ClipPath {
    val data = pathNode.attributes.getNamedItem("android:pathData").textContent
    return ClipPath(data)
}