package com.jzbrooks.vgo.core.graphic

import com.jzbrooks.vgo.core.Brush

interface PaintedElement : Element {
    val fill: Brush
    val fillRule: Path.FillRule
    val stroke: Brush
    val strokeWidth: Float
    val strokeLineCap: Path.LineCap
    val strokeLineJoin: Path.LineJoin
    val strokeMiterLimit: Float
}
