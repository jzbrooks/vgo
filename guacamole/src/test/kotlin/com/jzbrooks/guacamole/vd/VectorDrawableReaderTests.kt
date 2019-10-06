package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.assertk.extensions.containsKey
import com.jzbrooks.guacamole.assertk.extensions.containsKeys
import com.jzbrooks.guacamole.assertk.extensions.doesNotContainKey
import com.jzbrooks.guacamole.core.graphic.Graphic
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.Extra
import com.jzbrooks.guacamole.core.graphic.command.ClosePath
import com.jzbrooks.guacamole.core.graphic.command.CommandVariant
import com.jzbrooks.guacamole.core.graphic.command.LineTo
import com.jzbrooks.guacamole.core.graphic.command.MoveTo
import com.jzbrooks.guacamole.core.util.math.Point
import java.io.ByteArrayInputStream
import kotlin.test.Test

class VectorDrawableReaderTests {
    @Test
    fun testParseDimensions() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            assertThat(graphic.attributes["android:width"]).isEqualTo("24dp")
            assertThat(graphic.attributes["android:height"]).isEqualTo("24dp")
        }
    }

    @Test
    fun testParseMetadataDoesNotContainPathData() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.attributes).doesNotContainKey("android:pathData")
        }
    }

    @Test
    fun testParseMetadata() {
        javaClass.getResourceAsStream("/vd_visibilitystrike.xml").use {
            val graphic: Graphic = parse(it)

            val path = graphic.elements.first() as Path

            assertThat(path.attributes).containsKeys("android:name", "android:strokeWidth", "android:fillColor")
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

            assertThat(path.attributes).containsKey("android:name")
            assertThat(path.attributes["android:name"]).isEqualTo("strike_thru_path")
        }
    }

    @Test
    fun testParseComment() {
        ByteArrayInputStream("<vector><!-- test comment --></vector>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo(" test comment ")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        ByteArrayInputStream("<vector><bicycle /></vector>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        ByteArrayInputStream("<vector><bicycle></bicycle></vector>".toByteArray()).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra

            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).isEmpty()
        }
    }

    @Test
    fun testParseUnknownElementWithChildren() {
        val vectorText = """
            |<vector>
            |  <bicycle>
            |    <path android:pathData="M0,0l2,3Z" />
            |  </bicycle>
            |</vector>
            |""".trimMargin().toByteArray()

        val expectedChild = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f,0f))), LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))), ClosePath()))
        ByteArrayInputStream(vectorText).use {
            val graphic: Graphic = parse(it)

            val unknown = graphic.elements.first() as Extra



            assertThat(unknown.name).isEqualTo("bicycle")
            assertThat(unknown.elements).containsExactly(expectedChild)
        }
    }
}
