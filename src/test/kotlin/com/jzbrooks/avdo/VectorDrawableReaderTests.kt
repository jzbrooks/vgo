package com.jzbrooks.avdo

import com.jzbrooks.avdo.vd.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorDrawableReaderTests {
    @Test
    fun testParseDimensions() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            assertEquals(24, graphic.width)
            assertEquals(24, graphic.height)
        }
    }

    @Test
    fun testParsePaths() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            assertEquals("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z", graphic.paths.first().data)
            assertEquals(2, graphic.paths.size)
        }
    }
}
