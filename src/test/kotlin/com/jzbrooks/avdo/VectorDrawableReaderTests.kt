package com.jzbrooks.avdo

import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.Path
import com.jzbrooks.avdo.vd.parse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorDrawableReaderTests {
    @Test
    fun testParseDimensions() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            assertEquals(24, graphic.size.width.value)
            assertEquals(24, graphic.size.height.value)
        }
    }

    @Test
    fun testParsePaths() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path
            assertEquals("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z", path.data)
            assertEquals(3, graphic.elements.size)
        }
    }

    @Test
    fun testStoreNameForPath() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path
            assertTrue(path.metadata.containsKey("android:name"))
        }
    }
}
