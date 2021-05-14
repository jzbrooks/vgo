package com.jzbrooks.vgo.core.util.xml

import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (i in 0 until this.length) {
        list.add(this.item(i))
    }
    return list
}

fun NodeList.asSequence(): Sequence<Node> {
    var i = 0
    return generateSequence { this.item(i).also { i++ } }
}

fun NamedNodeMap.asSequence(): Sequence<Node> {
    var i = 0
    return generateSequence { this.item(i).also { i++ } }
}

fun NamedNodeMap.toMutableMap(): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    for (i in 0 until this.length) {
        val item = this.item(i)
        map[item.nodeName] = item.nodeValue
    }
    return map
}

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
