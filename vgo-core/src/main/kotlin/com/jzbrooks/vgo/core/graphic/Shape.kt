package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.util.math.Point

sealed interface Shape : PaintedElement {
    override fun accept(visitor: ElementVisitor) = visitor.visit(this)
}

data class Circle(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val cx: Float,
    val cy: Float,
    val r: Float,
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
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
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
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
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
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
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Polyline(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape

data class Polygon(
    override val id: String?,
    override val foreign: MutableMap<String, String>,
    val points: List<Point>,
    override val fill: Color,
    override val fillRule: Path.FillRule,
    override val stroke: Color,
    override val strokeWidth: Float,
    override val strokeLineCap: Path.LineCap,
    override val strokeLineJoin: Path.LineJoin,
    override val strokeMiterLimit: Float,
) : Shape
