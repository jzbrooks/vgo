package com.jzbrooks.vgo.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ColorTest {
    @Test
    fun testArgbHexString() {
        val color = Color(0x44ff1133u)
        assertThat(color.toHexString(Color.HexFormat.ARGB)).isEqualTo("#44ff1133")
    }

    @Test
    fun testRgbaHexString() {
        val color = Color(0x44ff1133u)
        assertThat(color.toHexString(Color.HexFormat.RGBA)).isEqualTo("#ff113344")
    }

    @Test
    fun testShortenedHex() {
        val color = Color(0xffffffffu)
        assertThat(color.toHexString(Color.HexFormat.ARGB)).isEqualTo("#fff")
    }
}
