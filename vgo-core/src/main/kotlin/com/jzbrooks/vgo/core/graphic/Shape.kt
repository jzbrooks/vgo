package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.util.math.Point

sealed interface Shape : PaintedElement {
    /**
     * Compatibility alias for [fill].
     */
    @Deprecated("Use fill instead.", ReplaceWith("fill"))
    var fillBrush: Brush
        get() = fill
        set(value) {
            fill = value
        }

    /**
     * Compatibility alias for [stroke].
     */
    @Deprecated("Use stroke instead.", ReplaceWith("stroke"))
    var strokeBrush: Brush
        get() = stroke
        set(value) {
            stroke = value
        }

    override var fill: Brush
    override var stroke: Brush

    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}

data class Circle(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val cx: Float,
    val cy: Float,
    val r: Float,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Ellipse(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Rect(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rx: Float,
    val ry: Float,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Line(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Polyline(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Polygon(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    override var fill: Brush,
    override val fillRule: Path.FillRule,
    override var stroke: Brush,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape
