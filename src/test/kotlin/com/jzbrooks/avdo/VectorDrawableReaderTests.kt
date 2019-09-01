package com.jzbrooks.avdo

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.jzbrooks.avdo.assertk.extensions.containsKey
import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.Path
import com.jzbrooks.avdo.vd.parse
import kotlin.test.Test

class VectorDrawableReaderTests {
    @Test
    fun testParseDimensions() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            assertThat(graphic.size.width.value).isEqualTo(24)
            assertThat(graphic.size.height.value).isEqualTo(24)
        }
    }

    @Test
    fun testParsePaths() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path
            assertThat(path.data).isEqualTo("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z")
            assertThat(graphic.elements).hasSize(3)
        }
    }

    @Test
    fun testStoreNameForPath() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path
            assertThat(path.metadata).containsKey("android:name")
        }
    }
}
