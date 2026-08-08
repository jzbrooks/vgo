package com.jzbrooks.vgo.core.util.element

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.transformation.TopDownTransformer

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

fun traverseElementTopDown(
    element: Element,
    transformer: (Element) -> Unit,
): Element {
    transformer(element)

    if (element is Group) {
        for (region in element.clipPaths.flatMap { it.regions }) {
            transformer(region)
        }
    }

    if (element is ContainerElement) {
        element.elements = element.elements.map { traverseElementTopDown(it, transformer) }
    }

    return element
}

/** Applies [transformers] in order while honoring their graphic and container lifecycle. */
fun traverseTopDown(
    graphic: Graphic,
    transformers: List<TopDownTransformer>,
): Graphic {
    traverseTopDownElement(graphic, transformers)

    return graphic
}

private fun traverseTopDownElement(
    element: Element,
    transformers: List<TopDownTransformer>,
) {
    val container = element as? ContainerElement
    var visitedCount = 0
    try {
        for (transformer in transformers) {
            if (container != null) visitedCount++
            element.accept(transformer)
        }

        if (element is Group) {
            for (region in element.clipPaths.flatMap { it.regions }) {
                traverseTopDownElement(region, transformers)
            }
        }

        if (container != null) {
            container.elements =
                container.elements.map { child ->
                    traverseTopDownElement(child, transformers)
                    child
                }
        }
    } finally {
        for (index in visitedCount - 1 downTo 0) transformers[index].exit(container!!)
    }
}
