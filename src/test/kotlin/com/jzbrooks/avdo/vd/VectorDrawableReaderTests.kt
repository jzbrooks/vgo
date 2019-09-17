package com.jzbrooks.avdo.vd

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.jzbrooks.avdo.assertk.extensions.containsKey
import com.jzbrooks.avdo.assertk.extensions.containsKeys
import com.jzbrooks.avdo.assertk.extensions.doesNotContainKey
import com.jzbrooks.avdo.graphic.Graphic
import com.jzbrooks.avdo.graphic.Path
import com.jzbrooks.avdo.graphic.command.*
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
    fun testParseMetadataDoesNotContainPathData() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.metadata).doesNotContainKey("android:pathData")
        }
    }

    @Test
    fun testParseMetadata() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.metadata).containsKeys("android:name", "android:strokeWidth", "android:fillColor")
        }
    }

    @Test
    fun testParsePaths() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path
            assertThat(path.commands).isEqualTo(
                    listOf(
                            MoveTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                            LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                            ClosePath()
                    )
            )
            assertThat(graphic.elements).hasSize(3)
        }
    }

    @Test
    fun testStoreNameForPath() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.metadata).containsKey("android:name")
            assertThat(path.metadata["android:name"]).isEqualTo("strike_thru_path")
        }
    }
}
