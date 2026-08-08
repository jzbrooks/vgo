package com.jzbrooks.vgo.core.transformation

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.ForeignPaint
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.PaintInheritance
import com.jzbrooks.vgo.core.graphic.PaintedElement
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.effectiveStroke
import com.jzbrooks.vgo.core.graphic.hasVisibleFillPaint
import com.jzbrooks.vgo.core.graphic.hasVisibleStrokePaint
import com.jzbrooks.vgo.core.graphic.usesMiterLimit

/**
 * Canonicalizes paint properties that can't affect rendering.
 *
 * A fill rule only qualifies a fill, and the stroke width, cap, join, and miter
 * limit only qualify a stroke — when the paint they qualify can't produce pixels,
 * their values are unobservable. Replacing them with stable values shrinks output, since
 * writers can elide them, and it lets [MergePaths] combine paths that render identically
 * but disagree about paint nobody can see.
 *
 * These canonical values are not necessarily a target format's defaults. Writers must
 * avoid emitting an unobservable qualifier when doing so would override an inherited
 * format value and make the canonical value observable.
 *
 * Whether an element's paint is dead depends on its ancestors in formats that inherit
 * paint, so [inheritance] supplies that rule.
 */
class RemoveRedundantPaintAttributes(
    private val inheritance: PaintInheritance = PaintInheritance.NONE,
) : TopDownTransformer {
    private val inheritedPaint = ArrayDeque<ForeignPaint>()

    override fun visit(graphic: Graphic) {
        inheritedPaint.clear()
        pushPaint(graphic)
        canonicalizeChildren(graphic)
    }

    override fun visit(group: Group) {
        pushPaint(group)
        canonicalizeChildren(group)
    }

    override fun visit(extra: Extra) {
        pushPaint(extra)
    }

    // Paint properties are immutable, so elements are rewritten by their parent
    // container rather than in place.
    override fun visit(shape: Shape) {}

    override fun visit(path: Path) {}

    override fun exit(container: ContainerElement) {
        inheritedPaint.removeLast()
        if (container is Graphic) check(inheritedPaint.isEmpty())
    }

    private fun pushPaint(container: ContainerElement) {
        val parentPaint = inheritedPaint.lastOrNull() ?: ForeignPaint.NONE
        inheritedPaint.addLast(inheritance.descend(container.foreign, parentPaint))
    }

    private fun canonicalizeChildren(container: ContainerElement) {
        container.elements = container.elements.map { canonicalize(it, inheritedPaint.last()) }
    }

    private fun canonicalize(
        element: Element,
        inherited: ForeignPaint,
    ): Element {
        // A named element can be the target of an animation that makes paint
        // visible later, so its dead paint isn't reliably dead.
        if (element !is PaintedElement || element.id != null) return element

        // A stroke paints nothing when it has no visible paint or no width. Clearing
        // both together keeps the pair consistent, which is what allows writers to
        // decide what to emit from the paint alone.
        val strokeIsDead = !element.hasVisibleStrokePaint(inherited) || element.strokeWidth == 0f
        val strokeLineJoin = if (strokeIsDead) Path.LineJoin.MITER else element.strokeLineJoin

        val paint =
            Paint(
                fillRule = if (element.hasVisibleFillPaint(inherited)) element.fillRule else Path.FillRule.NON_ZERO,
                stroke = if (strokeIsDead) Colors.TRANSPARENT else element.effectiveStroke,
                strokeWidth = if (strokeIsDead) 0f else element.strokeWidth,
                strokeLineCap = if (strokeIsDead) Path.LineCap.BUTT else element.strokeLineCap,
                strokeLineJoin = strokeLineJoin,
                strokeMiterLimit = if (strokeIsDead || !element.usesMiterLimit) 4f else element.strokeMiterLimit,
            )

        if (paint.matches(element)) return element

        return when (element) {
            is Path -> {
                element.copy(
                    fillRule = paint.fillRule,
                    stroke = paint.stroke,
                    strokeWidth = paint.strokeWidth,
                    strokeLineCap = paint.strokeLineCap,
                    strokeLineJoin = paint.strokeLineJoin,
                    strokeMiterLimit = paint.strokeMiterLimit,
                )
            }

            is Shape -> {
                element.withPaint(paint)
            }

            else -> {
                element
            }
        }
    }

    private data class Paint(
        val fillRule: Path.FillRule,
        val stroke: Brush,
        val strokeWidth: Float,
        val strokeLineCap: Path.LineCap,
        val strokeLineJoin: Path.LineJoin,
        val strokeMiterLimit: Float,
    ) {
        fun matches(element: PaintedElement) =
            element.fillRule == fillRule &&
                element.effectiveStroke == stroke &&
                element.strokeWidth == strokeWidth &&
                element.strokeLineCap == strokeLineCap &&
                element.strokeLineJoin == strokeLineJoin &&
                element.strokeMiterLimit == strokeMiterLimit
    }

    // Shape.copy doesn't carry fillBrush/strokeBrush, so both are restored explicitly.
    @Suppress("DEPRECATION")
    private fun Shape.withPaint(paint: Paint): Shape {
        val fillBrush = this.fillBrush
        // Every Shape narrows the deprecated stroke to Color. It tracks the brush when
        // the brush is a color and stays a placeholder when it's a gradient — in which
        // case the stroke is alive and the brush is unchanged anyway.
        val strokePlaceholder = paint.stroke as? Color ?: (stroke as Color)

        val copy =
            when (this) {
                is Circle -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }

                is Ellipse -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }

                is Rect -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }

                is Line -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }

                is Polyline -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }

                is Polygon -> {
                    copy(
                        fillRule = paint.fillRule,
                        stroke = strokePlaceholder,
                        strokeWidth = paint.strokeWidth,
                        strokeLineCap = paint.strokeLineCap,
                        strokeLineJoin = paint.strokeLineJoin,
                        strokeMiterLimit = paint.strokeMiterLimit,
                    )
                }
            }

        copy.fillBrush = fillBrush
        copy.strokeBrush = paint.stroke
        return copy
    }
}
