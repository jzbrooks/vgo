package com.jzbrooks.vgo.util.xml

import com.jzbrooks.vgo.core.graphic.Path
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.asSequence(): Sequence<Node> {
    var i = 0
    return generateSequence { item(i++) }
}

fun NodeList.toList() = asSequence().toList()

fun NamedNodeMap.asSequence(): Sequence<Node> {
    var i = 0
    return generateSequence { item(i++) }
}

fun NamedNodeMap.toMutableMap() =
    asSequence()
        .associate { it.nodeName to it.nodeValue }
        .toMutableMap()

fun NamedNodeMap.removeOrNull(key: String): Node? {
    val value = getNamedItem(key)

    if (value != null) {
        removeNamedItem(key)
    }

    return value
}

fun NamedNodeMap.removeFloatOrNull(key: String): Float? {
    val value = getNamedItem(key)?.nodeValue?.toFloatOrNull()

    if (value != null) {
        removeNamedItem(key)
    }

    return value
}

fun NamedNodeMap.extractLineCap(key: String) =
    when (removeOrNull(key)?.nodeValue) {
        "round" -> Path.LineCap.ROUND
        "square" -> Path.LineCap.SQUARE
        else -> Path.LineCap.BUTT
    }

fun NamedNodeMap.extractLineJoin(key: String) =
    when (removeOrNull(key)?.nodeValue) {
        "round" -> Path.LineJoin.ROUND
        "bevel" -> Path.LineJoin.BEVEL
        "arcs" -> Path.LineJoin.ARCS
        "miter-clip" -> Path.LineJoin.MITER_CLIP
        else -> Path.LineJoin.MITER
    }
