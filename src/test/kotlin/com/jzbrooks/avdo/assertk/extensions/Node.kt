package com.jzbrooks.avdo.assertk.extensions

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