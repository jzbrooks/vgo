package com.jzbrooks.guacamole.util.xml

import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.toList(): List<Node> {
    val list = mutableListOf<Node>()
    for (i in 0 until this.length) {
        list.add(this.item(i))
    }
    return list.toList()
}