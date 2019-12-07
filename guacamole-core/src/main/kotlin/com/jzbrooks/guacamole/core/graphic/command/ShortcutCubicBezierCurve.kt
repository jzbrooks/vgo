package com.jzbrooks.guacamole.core.graphic.command

import com.jzbrooks.guacamole.core.util.math.Point

data class ShortcutCubicBezierCurve(
        override var variant: CommandVariant,
        override var parameters: List<Parameter>
) : ParameterizedCommand<ShortcutCubicBezierCurve.Parameter> {
    data class Parameter(var endControl: Point, var end: Point)
}