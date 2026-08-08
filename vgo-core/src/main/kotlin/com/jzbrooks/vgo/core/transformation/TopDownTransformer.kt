package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.ElementVisitor
import com.jzbrooks.vgo.core.graphic.Graphic

/**
 * A transformation applied from a [Graphic] root to its leaves.
 *
 * [ElementVisitor.visit] is called before an element's descendants, and [exit] is called
 * afterward for containers, allowing implementations to maintain ancestor-scoped state.
 */
interface TopDownTransformer : ElementVisitor {
    /** Called after [container]'s descendants have been visited. */
    fun exit(container: ContainerElement) {}
}
