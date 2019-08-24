package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.Group
import com.jzbrooks.avdo.Path
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

fun parse(input: InputStream): VectorDrawable {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
    document.documentElement.normalize()

    val width = document.documentElement.attributes.getNamedItem("android:width").textContent.removeSuffix("dp").toInt()
    val height = document.documentElement.attributes.getNamedItem("android:height").textContent.removeSuffix("dp").toInt()

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

    return VectorDrawable(paths, groups, width, height)
}
