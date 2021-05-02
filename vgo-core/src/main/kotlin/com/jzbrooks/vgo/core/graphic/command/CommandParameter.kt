package com.jzbrooks.vgo.core.graphic.command

import com.jzbrooks.vgo.core.util.math.Point

/**
 * A complex command parameter has multiple
 * fields in its parameter, but also and end point.
 */
interface CommandParameter {
    var end: Point
}
