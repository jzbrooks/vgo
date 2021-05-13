package com.jzbrooks.vgo.vd

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.assertk.containsKeys
import com.jzbrooks.vgo.util.assertk.doesNotContainKey
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class VectorDrawableReaderTests {
    private lateinit var node: Node

    @BeforeEach
    fun setup() {
        javaClass.getResourceAsStream("/visibility_strike.xml").use { input ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input)
            document.normalize()
            node = document.firstChild
        }
    }

    @Test
    fun testParseDimensions() {
        val graphic: Graphic = parse(node)

        assertThat(graphic.foreign["android:width"]).isEqualTo("24dp")
        assertThat(graphic.foreign["android:height"]).isEqualTo("24dp")
    }

    @Test
    fun testParseMetadataDoesNotContainPathData() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.foreign).doesNotContainKey("android:pathData")
    }

    @Test
    fun testParseMetadata() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.id).isNotNull()
        assertThat(path.foreign).containsKeys("android:strokeWidth", "android:fillColor")
    }

    @Test
    fun testParsePaths() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path
        assertThat(path.commands).isEqualTo(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                ClosePath
            )
        )
        assertThat(graphic.elements).hasSize(3)
    }

    @Test
    fun testStoreNameForPath() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path.id).isEqualTo("strike_thru_path")
    }

    @Test
    fun testIgnoreComment() {
        val commentDocument = ByteArrayInputStream("<vector><!-- test comment --></vector>".toByteArray()).use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(commentDocument.firstChild)

        assertThat(graphic.elements).isEmpty()
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        val unknownElementDocument = ByteArrayInputStream("<vector><bicycle /></vector>".toByteArray()).use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).isEmpty()
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        val unknownElementDocument = ByteArrayInputStream("<vector><bicycle></bicycle></vector>".toByteArray()).use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).isEmpty()
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

        val expectedChild = Path(listOf(MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))), LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))), ClosePath))

        val unknownElementDocument = ByteArrayInputStream(vectorText).use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown.name).isEqualTo("bicycle")
        assertThat(unknown.elements).containsExactly(expectedChild)
    }

    @Test
    fun testPathDataSpecifiedWithResourceIsUntouched() {
        val vectorText = """
            |<vector>
            |  <path android:pathData="@string/path_data" />
            |</vector>
            |""".trimMargin().toByteArray()

        val unknownElementDocument = ByteArrayInputStream(vectorText).use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                documentElement.normalize()
            }
        }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val path = graphic.elements.first() as Path

        assertThat(path.commands).isEmpty()
        assertThat(path.foreign).contains("android:pathData", "@string/path_data")
    }
}
