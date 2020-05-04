package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

data class ShortcutCubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : CubicCurve<ShortcutCubicBezierCurve.Parameter> {
    data class Parameter(var endControl: Point, override var end: Point) : EndPoint
}