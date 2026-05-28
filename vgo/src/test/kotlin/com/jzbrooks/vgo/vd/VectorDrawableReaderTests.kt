package com.jzbrooks.vgo.vd

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsNone
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.key
import assertk.assertions.prop
import assertk.assertions.single
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.util.element.createPath
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

        assertThat(path.foreign.keys).containsNone("android:pathData")
    }

    @Test
    fun testParseMetadata() {
        val graphic: Graphic = parse(node)

        assertThat(graphic::elements).index(0).isInstanceOf(Path::class).all {
            prop(Path::id).isNotNull()
            prop(Path::strokeWidth).isEqualTo(1f)
            prop(Path::fill).isEqualTo(Colors.BLACK)
        }
    }

    @Test
    fun testParsePaths() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path
        assertThat(path::commands).isEqualTo(
            listOf(
                MoveTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(3.27f, 3f))),
                LineTo(CommandVariant.ABSOLUTE, listOf(Point(2f, 4.27f))),
                ClosePath,
            ),
        )
        // The fixture is `[path, clip-path, path]` at the root; positional VD scope
        // turns this into `[path, synthGroup(clipPaths=[c], elements=[path])]`.
        assertThat(graphic::elements).hasSize(2)
        assertThat(graphic::elements)
            .index(1)
            .isInstanceOf(Group::class)
            .prop(Group::clipPaths)
            .hasSize(1)
    }

    @Test
    fun testStoreNameForPath() {
        val graphic: Graphic = parse(node)

        val path = graphic.elements.first() as Path

        assertThat(path::id).isEqualTo("strike_thru_path")
    }

    @Test
    fun testIgnoreComment() {
        val commentDocument =
            ByteArrayInputStream("<vector><!-- test comment --></vector>".toByteArray()).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(commentDocument.firstChild)

        assertThat(graphic::elements).isEmpty()
    }

    @Test
    fun testParseSelfClosedUnknownElementWithoutChildren() {
        val unknownElementDocument =
            ByteArrayInputStream("<vector><bicycle /></vector>".toByteArray()).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown::name).isEqualTo("bicycle")
        assertThat(unknown::elements).isEmpty()
    }

    @Test
    fun testParseUnknownElementWithoutChildren() {
        val unknownElementDocument =
            ByteArrayInputStream("<vector><bicycle></bicycle></vector>".toByteArray()).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown::name).isEqualTo("bicycle")
        assertThat(unknown::elements).isEmpty()
    }

    @Test
    fun testMinimumFillAlphaIsPreferred() {
        val vectorText =
            """
            |<vector>
            |  <path android:pathData="M0,0l2,3Z" android:fillColor="#FF00FF00" android:fillAlpha="0.5" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val path = graphic.elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0x8000FF00u))
    }

    @Test
    fun testMinimumStrokeAlphaIsPreferred() {
        val vectorText =
            """
            |<vector>
            |  <path android:pathData="M0,0l2,3Z" android:strokeColor="#FF00FF00" android:strokeAlpha="0.5" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val path = graphic.elements.first() as Path

        assertThat(path::stroke).isEqualTo(Color(0x8000FF00u))
    }

    @Test
    fun testParseUnknownElementWithChildren() {
        val vectorText =
            """
            |<vector>
            |  <bicycle>
            |    <path android:pathData="M0,0l2,3Z" />
            |  </bicycle>
            |</vector>
            |
            """.trimMargin().toByteArray()

        val expectedChild =
            createPath(
                listOf(
                    MoveTo(CommandVariant.ABSOLUTE, listOf(Point(0f, 0f))),
                    LineTo(CommandVariant.RELATIVE, listOf(Point(2f, 3f))),
                    ClosePath,
                ),
                fill = Colors.TRANSPARENT,
                strokeWidth = 0f,
            )

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val unknown = graphic.elements.first() as Extra

        assertThat(unknown::name).isEqualTo("bicycle")
        assertThat(unknown::elements).containsExactly(expectedChild)
    }

    @Test
    fun testPathDataSpecifiedWithResourceIsUntouched() {
        val vectorText =
            """
            |<vector>
            |  <path android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic: Graphic = parse(unknownElementDocument.firstChild)

        val path = graphic.elements.first() as Path

        assertThat(path::commands).isEmpty()
        assertThat(path::foreign).contains("android:pathData", "@string/path_data")
    }

    @Test
    fun testFullColorParsed() {
        val vectorText =
            """
            |<vector>
            |  <path android:fillColor="#88ff9988" android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val path = parse(unknownElementDocument.firstChild).elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0x88FF9988u))
    }

    @Test
    fun testColorWithoutSpecifiedAlphaParsed() {
        val vectorText =
            """
            |<vector>
            |  <path android:fillColor="#ff9988" android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val path = parse(unknownElementDocument.firstChild).elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFF9988u))
    }

    @Test
    fun testShortenedColorParsed() {
        val vectorText =
            """
            |<vector>
            |  <path android:fillColor="#fff" android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val path = parse(unknownElementDocument.firstChild).elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0xFFFFFFFFu))
    }

    @Test
    fun testLesserSpecifiedAlphaIsTaken() {
        val vectorText =
            """
            |<vector>
            |  <path android:fillColor="#88ff9988" android:fillAlpha="0.1" android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val path = parse(unknownElementDocument.firstChild).elements.first() as Path

        assertThat(path::fill).isEqualTo(Color(0x1AFF9988u))
    }

    @Test
    fun testLinearGradientFillParsed() {
        val document =
            javaClass.getResourceAsStream("/gradient_linear.xml").use { input ->
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input).apply {
                    normalize()
                }
            }

        val graphic: Graphic = parse(document.firstChild)
        val path = graphic.elements.first() as Path

        val expected =
            LinearGradient(
                startX = 0f,
                startY = 0f,
                endX = 24f,
                endY = 0f,
                stops =
                    listOf(
                        GradientStop(0f, Color(0xFFB125EAu)),
                        GradientStop(0.5f, Color(0xFF833FEFu)),
                        GradientStop(1f, Color(0xFF008AFFu)),
                    ),
            )

        assertThat(path::fill).isEqualTo(expected)
    }

    @Test
    fun testThemeReferencedColorIgnored() {
        val vectorText =
            """
            |<vector>
            |  <path android:fillColor="?attrs/dark" android:fillAlpha="0.1" android:pathData="@string/path_data" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val unknownElementDocument =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val path = parse(unknownElementDocument.firstChild).elements.first() as Path

        assertThat(path::foreign).key("android:fillColor").isEqualTo("?attrs/dark")
        assertThat(path::foreign).key("android:fillAlpha").isEqualTo("0.1")
    }

    @Test
    fun testLeadingClipPathsProduceSyntheticSubGroup() {
        // With positional VD scope, `[clip, clip, path]` inside a group becomes
        // a single synthetic sub-group carrying both clips and the path.
        val vectorText =
            """
            |<vector>
            |  <group>
            |    <clip-path android:pathData="M0,0h10v10h-10z" />
            |    <clip-path android:pathData="M5,5h10v10h-10z" />
            |    <path android:pathData="M0,0l2,3Z" android:fillColor="#FF0000" />
            |  </group>
            |</vector>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic = parse(document.firstChild)

        assertThat(graphic, "graphic")
            .prop(Graphic::elements)
            .single()
            .isInstanceOf<Group>()
            .all {
                prop(Group::clipPaths).isEmpty()
                prop(Group::elements).single().isInstanceOf<Group>().all {
                    prop(Group::clipPaths).hasSize(2)
                    prop(Group::elements).single().isInstanceOf<Path>()
                }
            }
    }

    @Test
    fun testInterleavedClipPathProducesSyntheticSubGroup() {
        val vectorText =
            """
            |<vector>
            |  <group>
            |    <path android:pathData="M0,0l2,3Z" />
            |    <clip-path android:pathData="M0,0h10v10h-10z" />
            |    <path android:pathData="M5,5l1,1Z" />
            |  </group>
            |</vector>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic = parse(document.firstChild)

        assertThat(graphic, "graphic")
            .prop(Graphic::elements)
            .single()
            .isInstanceOf<Group>()
            .all {
                prop(Group::clipPaths).isEmpty()
                prop(Group::elements).all {
                    hasSize(2)
                    first().isInstanceOf<Path>()
                    index(1).isInstanceOf<Group>().all {
                        prop(Group::clipPaths).hasSize(1)
                        prop(Group::elements).single().isInstanceOf<Path>()
                    }
                }
            }
    }

    @Test
    fun testTopLevelClipPathProducesSyntheticGroup() {
        val vectorText =
            """
            |<vector>
            |  <path android:pathData="M0,0l2,3Z" />
            |  <clip-path android:pathData="M0,0h10v10h-10z" />
            |  <path android:pathData="M5,5l1,1Z" />
            |</vector>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic = parse(document.firstChild)

        assertThat(graphic, "graphic")
            .prop(Graphic::elements)
            .all {
                hasSize(2)
                first().isInstanceOf<Path>()
                index(1).isInstanceOf<Group>().all {
                    prop(Group::clipPaths).hasSize(1)
                    prop(Group::elements).single().isInstanceOf<Path>()
                }
            }
    }

    @Test
    fun testReClipMidStreamProducesNestedSyntheticGroups() {
        val vectorText =
            """
            |<vector>
            |  <group>
            |    <clip-path android:pathData="M0,0h10v10h-10z" />
            |    <path android:pathData="M0,0l2,3Z" />
            |    <clip-path android:pathData="M5,5h10v10h-10z" />
            |    <path android:pathData="M5,5l1,1Z" />
            |  </group>
            |</vector>
            |
            """.trimMargin().toByteArray()

        val document =
            ByteArrayInputStream(vectorText).use {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it).apply {
                    documentElement.normalize()
                }
            }

        val graphic = parse(document.firstChild)

        assertThat(graphic, "graphic")
            .prop(Graphic::elements)
            .single()
            .isInstanceOf<Group>()
            .all {
                prop(Group::clipPaths).isEmpty()
                prop(Group::elements).single().isInstanceOf<Group>().all {
                    prop(Group::clipPaths).hasSize(1)
                    prop(Group::elements).all {
                        hasSize(2)
                        index(0).isInstanceOf<Path>()
                        index(1).isInstanceOf<Group>().all {
                            prop(Group::clipPaths).hasSize(1)
                            prop(Group::elements).single().isInstanceOf<Path>()
                        }
                    }
                }
            }
    }
}
