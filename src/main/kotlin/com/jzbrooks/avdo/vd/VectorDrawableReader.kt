package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.Dimension
import com.jzbrooks.avdo.graphic.Group
import com.jzbrooks.avdo.graphic.Path
import com.jzbrooks.avdo.graphic.Size
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

    val paths = mutableListOf<Path>()
    val pathList = document.getElementsByTagName("path")
    for (index in 0 until pathList.length) {
        val item = pathList.item(index)

        val data = item.attributes.getNamedItem("android:pathData").textContent
        val strokeWidth = item.attributes.getNamedItem("android:strokeWidth").textContent.toInt()
        paths.add(Path(data, strokeWidth))
    }

    val groups = mutableListOf<Group>()
    val groupList = document.getElementsByTagName("group")
    for (index in 0 until groupList.length) {
        val item = groupList.item(index)
        val groupPathList = mutableListOf<Path>()

        for (child in 0 until item.childNodes.length) {
            val childPath = item.childNodes.item(child)
            val data = childPath.attributes.getNamedItem("android:pathData").textContent
            val strokeWidth = childPath.attributes.getNamedItem("android:strokeWidth").textContent.toInt()
            groupPathList.add(Path(data, strokeWidth))
        }

        groups.add(Group(groupPathList))
    }

    return VectorDrawable(paths, groups, Size(Dimension(width, widthDimension), Dimension(height, heightDimension)))
}
