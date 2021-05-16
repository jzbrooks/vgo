package com.jzbrooks.vgo.core

@JvmInline
value class Color(private val argb: UInt) {
    val alpha: UByte
        get() = (argb shr 24).toUByte()

    val red: UByte
        get() = (argb shr 16).toUByte()

    val green: UByte
        get() = (argb shr 8).toUByte()

    val blue: UByte
        get() = argb.toUByte()

    operator fun component1() = alpha
    operator fun component2() = red
    operator fun component3() = green
    operator fun component4() = blue

    fun toHexString(format: HexFormat): String {
        return if (alpha != 0xFF.toUByte()) {
            val pattern = "#%02x%02x%02x%02x"
            when (format) {
                HexFormat.ARGB -> pattern.format(
                    alpha.toInt(),
                    red.toInt(),
                    green.toInt(),
                    blue.toInt(),
                )
                HexFormat.RGBA -> pattern.format(
                    red.toInt(),
                    green.toInt(),
                    blue.toInt(),
                    alpha.toInt(),
                )
            }
        } else {
            val pattern = if (red == green && red == blue) {
                "#%02x%02x%02x"
            } else {
                "#%02x%02x%02x"
            }

            val hexColor = pattern.format(red.toInt(), green.toInt(), blue.toInt())

            if (hexColor.drop(2).all { it == hexColor[1] }) {
                hexColor.dropLast(3)
            } else {
                hexColor
            }
        }
    }

    enum class HexFormat {
        RGBA,
        ARGB,
    }
}

object Colors {
    val BLACK = Color(0xFF000000u)
    val TRANSPARENT = Color(0x00000000u)
}
