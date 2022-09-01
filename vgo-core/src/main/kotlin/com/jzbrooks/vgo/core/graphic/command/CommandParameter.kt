package com.jzbrooks.vgo.core.graphic.command

import dev.romainguy.kotlin.math.Float2

/**
 * A complex command parameter has multiple
 * fields in its parameter, but also and end point.
 */
interface CommandParameter {
    var end: Float2
}
