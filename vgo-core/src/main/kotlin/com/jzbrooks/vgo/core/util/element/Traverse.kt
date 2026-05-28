package com.jzbrooks.vgo.core.util.element

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Group

fun traverseBottomUp(
    element: Element,
    transformer: (Element) -> Unit,
): Element {
    if (element is ContainerElement) {
        element.elements = element.elements.map { traverseBottomUp(it, transformer) }
    }

    if (element is Group) {
        for (clipPath in element.clipPaths) {
            for (region in clipPath.regions) transformer(region)
        }
    }

    transformer(element)

    return element
}

fun traverseTopDown(
    element: Element,
    transformer: (Element) -> Unit,
): Element {
    transformer(element)

    if (element is Group) {
        for (clipPath in element.clipPaths) {
            for (region in clipPath.regions) transformer(region)
        }
    }

    if (element is ContainerElement) {
        element.elements = element.elements.map { traverseTopDown(it, transformer) }
    }

    return element
}
