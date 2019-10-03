package com.jzbrooks.guacamole.util.xml

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

fun NamedNodeMap.toMap(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (i in 0 until this.length) {
        val item = this.item(i)
        map[item.nodeName] = item.nodeValue
    }
    return map
}
