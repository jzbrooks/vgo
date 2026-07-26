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
 * The true fill paint. [Shape] surfaces gradients through [Shape.fillBrush];
 * its [fill] is a deprecated placeholder.
 */
val PaintedElement.effectiveFill: Brush
    get() = if (this is Shape) fillBrush else fill

/**
 * The true stroke paint. [Shape] surfaces gradients through [Shape.strokeBrush];
 * its [PaintedElement.stroke] is a deprecated placeholder.
 */
val PaintedElement.effectiveStroke: Brush
    get() = if (this is Shape) strokeBrush else stroke

/**
 * Whether the fill paint can produce pixels, which determines whether properties
 * that only qualify the fill — like [fillRule] — are observable.
 *
 * This describes the *paint* alone. It intentionally says nothing about geometry.
 */
val PaintedElement.hasVisibleFillPaint: Boolean
    get() = effectiveFill.isVisible || hasUnmodeledPaint("fill")

/**
 * Whether the stroke paint can produce pixels, which determines whether properties
 * that only qualify the stroke — like [strokeLineCap] — are observable.
 *
 * This describes the *paint* alone: a visible paint with a zero [strokeWidth] still
 * counts, because the width has to survive into output for formats whose default
 * width is nonzero.
 */
val PaintedElement.hasVisibleStrokePaint: Boolean
    get() = effectiveStroke.isVisible || hasUnmodeledPaint("stroke")

/**
 * Whether [strokeMiterLimit] is observable. It's the length at which a miter join
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
 * elements simply keep their paint attributes.
 */
private fun PaintedElement.hasUnmodeledPaint(channel: String): Boolean =
    foreign.keys.any { it.contains(channel, ignoreCase = true) } ||
        foreign["style"]?.contains(channel, ignoreCase = true) == true ||
        foreign.values.any { it.contains("url(") }
