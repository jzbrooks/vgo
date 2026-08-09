package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Gradient

interface PaintedElement : Element {
    val fill: Brush
    val fillRule: Path.FillRule
    val stroke: Brush
    val strokeWidth: Float
    val strokeLineCap: Path.LineCap
    val strokeLineJoin: Path.LineJoin
    val strokeMiterLimit: Float
}

/**
 * Paint an ancestor applies that the intermediate representation can't model, per
 * channel. A descendant of such an ancestor renders with that paint unless it declares
 * its own, so its typed fields — which the reader filled with a format default it had
 * no better answer for — can't be trusted to say the channel is dead.
 */
data class ForeignPaint(
    val fill: Boolean = false,
    val stroke: Boolean = false,
) {
    companion object {
        val NONE = ForeignPaint()
    }
}

/**
 * How a format carries unmodelable paint from a container to its descendants. SVG
 * inherits paint as a presentation attribute; vector drawables and ImageVector paint
 * each element outright, so [NONE] is the whole truth for them rather than merely a
 * safe default.
 */
fun interface PaintInheritance {
    fun descend(
        foreign: Map<String, String>,
        current: ForeignPaint,
    ): ForeignPaint

    companion object {
        val NONE = PaintInheritance { _, _ -> ForeignPaint.NONE }
    }
}

/**
 * Whether the fill paint can produce pixels, which determines whether properties
 * that only qualify the fill — like [fillRule] — are observable.
 *
 * This describes the *paint* alone. It intentionally says nothing about geometry.
 */
fun PaintedElement.hasVisibleFillPaint(inherited: ForeignPaint = ForeignPaint.NONE): Boolean =
    inherited.fill || fill.isVisible || hasForeignPaint("fill")

/**
 * Whether the stroke paint can produce pixels, which determines whether properties
 * that only qualify the stroke — like [strokeLineCap] — are observable.
 *
 * This describes the *paint* alone: a visible paint with a zero [strokeWidth] still
 * counts, because the width has to survive into output for formats whose default
 * width is nonzero.
 */
fun PaintedElement.hasVisibleStrokePaint(inherited: ForeignPaint = ForeignPaint.NONE): Boolean =
    inherited.stroke || stroke.isVisible || hasForeignPaint("stroke")

/**
 * Whether [PaintedElement.strokeMiterLimit] is observable. It's the length at which a miter join
 * degrades to a bevel, or — for [Path.LineJoin.MITER_CLIP] — the point at which the
 * miter is clipped. No other join reads it.
 */
val PaintedElement.usesMiterLimit: Boolean
    get() = strokeLineJoin == Path.LineJoin.MITER || strokeLineJoin == Path.LineJoin.MITER_CLIP

/**
 * Gradient stops aren't introspected, so a gradient is always treated as visible.
 */
private val Brush.isVisible: Boolean
    get() =
        when (this) {
            is Color -> alpha != 0.toUByte()
            is Gradient -> true
        }

/**
 * Whether paint that the intermediate representation doesn't model may still be
 * lurking in [foreign] for the given channel — an unparsable color like
 * `android:fillColor="?attr/colorPrimary"`, an alpha or opacity component, or a
 * paint server reference restored by the reader when it couldn't be resolved.
 * Such an element renders paint that its typed fields don't describe, so nothing
 * about that channel can be elided.
 *
 * Deliberately over-approximate: substring matching also catches attributes that
 * merely mention a channel without painting it, like `stroke-dasharray`. Those
 * elements simply keep their paint attributes. A color the element declares without
 * naming a channel counts for both, since it's what a reference like SVG's
 * `currentColor` resolves to.
 */
private fun PaintedElement.hasForeignPaint(channel: String): Boolean =
    foreign.keys.any { it.contains(channel, ignoreCase = true) || it.contains("color", ignoreCase = true) } ||
        foreign["style"]?.contains(channel, ignoreCase = true) == true ||
        foreign.values.any { it.contains("url(") }
