package com.jzbrooks.vgo.core.util.element

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element

fun traverseBottomUp(
    element: Element,
    transformer: (Element) -> Unit,
): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map { traverseBottomUp(it, transformer) }
    }

    transformer(element)

    return element
}

fun traverseTopDown(
    element: Element,
    transformer: (Element) -> Unit,
): Element {
    transformer(element)

    if (element is ContainerElement) {
        element.elements = element.elements.map { traverseTopDown(it, transformer) }
    }

    return element
}
