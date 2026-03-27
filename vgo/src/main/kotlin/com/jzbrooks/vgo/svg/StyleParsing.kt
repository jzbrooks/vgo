package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Path

internal fun String.parseStyleAttribute(): Map<String, String> =
    split(';')
        .mapNotNull { property ->
            val colonIndex = property.indexOf(':')
            if (colonIndex > 0) {
                property.substring(0, colonIndex).trim() to property.substring(colonIndex + 1).trim()
            } else {
                null
            }
        }.toMap()

internal val PRESENTATION_ATTRIBUTES: Set<String> =
    hashSetOf("fill", "fill-rule", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin", "stroke-miterlimit")

internal fun parseColorValue(
    value: String,
    default: Color,
): Color {
    if (value == "none") return Color(0x00000000u)

    val hex =
        if (value.startsWith("rgb")) {
            val (r, g, b) =
                value
                    .removePrefix("rgb(")
                    .trimEnd(')')
                    .split(',')
                    .map { it.trim().toShort() }

            "%02x%02x%02x".format(r, g, b)
        } else if (value.startsWith("#")) {
            val hex = value.trim('#')
            if (hex.length != 3) hex else ("${hex[0]}" + hex[0] + hex[1] + hex[1] + hex[2] + hex[2])
        } else {
            return Colors.COLORS_BY_NAMES[value] ?: default
        }

    return Color(hex.toUInt(radix = 16) or 0xFF000000u)
}

internal fun Map<String, String>.extractColor(
    key: String,
    default: Color,
): Color? {
    val value = this[key] ?: return null
    return parseColorValue(value, default)
}

internal fun Map<String, String>.extractFillRule(key: String): Path.FillRule? =
    when (this[key]) {
        "evenodd" -> Path.FillRule.EVEN_ODD
        "nonzero" -> Path.FillRule.NON_ZERO
        else -> null
    }

internal fun Map<String, String>.extractLineCap(key: String): Path.LineCap? =
    when (this[key]) {
        "round" -> Path.LineCap.ROUND
        "square" -> Path.LineCap.SQUARE
        "butt" -> Path.LineCap.BUTT
        else -> null
    }

internal fun Map<String, String>.extractLineJoin(key: String): Path.LineJoin? =
    when (this[key]) {
        "round" -> Path.LineJoin.ROUND
        "bevel" -> Path.LineJoin.BEVEL
        "arcs" -> Path.LineJoin.ARCS
        "miter-clip" -> Path.LineJoin.MITER_CLIP
        "miter" -> Path.LineJoin.MITER
        else -> null
    }
