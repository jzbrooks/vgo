package com.jzbrooks.vgo.assertk.extensions

import assertk.Assert
import assertk.assertions.support.fail
import org.w3c.dom.Node

fun <T> Assert<Node>.hasValue(other: T) = given { actual ->
    if (actual.nodeValue == other) {
        return
    }

    fail(other, actual)
}

fun <T> Assert<Node>.hasName(other: T) = given { actual ->
    if (actual.nodeName == other) {
        return
    }

    fail(other, actual)
}

/**
 * Asserts the node names in the list appear in the same order as the parameters
 */
fun Assert<List<Node>>.hasNames(vararg names: String) = given { actual ->
    for ((i, name) in names.withIndex()) {
        if (actual[i].nodeName != name)
            fail(names, actual)
    }
}