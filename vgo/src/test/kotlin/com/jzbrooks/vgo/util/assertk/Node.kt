package com.jzbrooks.vgo.util.assertk

import assertk.Assert
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.support.expected
import assertk.assertions.support.fail
import assertk.assertions.support.show
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node

fun <T> Assert<Node>.hasValue(other: T) =
    given { actual ->
        if (actual.nodeValue != other) {
            fail(other, actual)
        }
    }

fun <T> Assert<Node>.hasName(other: T) =
    given { actual ->
        if (actual.nodeName != other) {
            fail(other, actual)
        }
    }

fun Assert<Node>.attribute(name: String) = prop("attributes", Node::getAttributes).item(name)

fun Assert<Node>.hasAttribute(name: String) =
    given { actual ->
        if (actual.attributes.getNamedItem(name) == null) {
            expected("to contain:${show(name)} but was:${show(actual.attributes)}")
        }
    }

fun Assert<Node>.doesNotHaveAttribute(name: String) =
    given { actual ->
        if (actual.attributes.getNamedItem(name) != null) {
            expected("to not contain:${show(name)} but was:${show(actual.attributes)}")
        }
    }

fun Assert<NamedNodeMap>.item(name: String) =
    transform("[$name]") { actual ->
        actual.getNamedItem(name)
    }.isNotNull().prop("nodeValue", Node::getNodeValue).isNotNull()

/**
 * Asserts the node names in the list appear in the same order as the parameters
 */
fun Assert<List<Node>>.hasNames(vararg names: String) =
    given { actual ->
        for ((i, name) in names.withIndex()) {
            if (actual[i].nodeName != name) {
                fail(names, actual)
            }
        }
    }
