package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.graphic.ForeignPaint
import com.jzbrooks.vgo.core.graphic.PaintInheritance

/**
 * SVG paint is an inherited presentation attribute, so paint the reader couldn't lift
 * into a typed field — a paint server reference, or a keyword like `currentColor` that
 * resolves outside the document — keeps painting every descendant that doesn't declare
 * paint of its own. The reader leaves such a value on the element that carries it and
 * hands descendants the CSS initial value, so an ancestor is the only place the truth
 * about their paint is recorded.
 *
 * The single source of truth for the rule: [SvgOptimizationRegistry] configures the
 * transformations with it, and [ScalableVectorGraphicWriter] threads it through the
 * inherited style it already tracks.
 */
class SvgPaintInheritance : PaintInheritance {
    override fun descend(
        foreign: Map<String, String>,
        current: ForeignPaint,
    ): ForeignPaint {
        // The order of concatenation is important for precedence.
        val declarations = foreign + (foreign["style"]?.parseStyleAttribute() ?: emptyMap())

        return ForeignPaint(
            fill = declarations.paintIsForeign("fill", current.fill),
            stroke = declarations.paintIsForeign("stroke", current.stroke),
        )
    }

    // A declaration the color parser understands replaces whatever an ancestor was
    // painting, so it clears the flag as surely as an unmodelable one sets it.
    private fun Map<String, String>.paintIsForeign(
        key: String,
        current: Boolean,
    ): Boolean {
        val declared = this[key] ?: return current
        return parseColorOrNull(declared) == null
    }
}
