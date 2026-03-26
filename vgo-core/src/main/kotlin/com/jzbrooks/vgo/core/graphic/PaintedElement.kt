package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Paint

interface PaintedElement : Element {
    val fill: Paint
    val fillRule: Path.FillRule
    val stroke: Paint
    val strokeWidth: Float
    val strokeLineCap: Path.LineCap
    val strokeLineJoin: Path.LineJoin
    val strokeMiterLimit: Float
}
