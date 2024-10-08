package com.jzbrooks.vgo.vd

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.util.assertk.hasName
import com.jzbrooks.vgo.util.assertk.hasNames
import com.jzbrooks.vgo.util.assertk.hasValue
import com.jzbrooks.vgo.util.element.createPath
import com.jzbrooks.vgo.util.xml.toList
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class VectorDrawableWriterTests {
    @Test
    fun testRootElementIsVector() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            assertThat(output.firstChild, "first document element").hasName("vector")
        }
    }

    @Test
    fun testIsViewboxWritten() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.firstChild.attributes

            assertThat(rootAttributes.getNamedItem("android:viewportWidth")).hasValue("24")
            assertThat(rootAttributes.getNamedItem("android:viewportHeight")).hasValue("24")
        }
    }

    @Test
    fun testIsSizeWritten() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.firstChild.attributes

            assertThat(rootAttributes.getNamedItem("android:width").nodeValue).startsWith("24")
            assertThat(rootAttributes.getNamedItem("android:height").nodeValue).startsWith("24")
        }
    }

    @Test
    fun testPathsWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()
            val transformGroupNodes =
                output.firstChild.firstChild.childNodes
                    .toList()

            assertThat(firstGenNodes + transformGroupNodes)
                .transform("path element count") { it.count { item -> item.nodeName == "path" } }
                .isEqualTo(2)
        }
    }

    @Test
    fun testUnitsWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootAttributes = output.firstChild.attributes

            assertThat(rootAttributes.getNamedItem("android:height").nodeValue).endsWith("dp")
            assertThat(rootAttributes.getNamedItem("android:width").nodeValue).endsWith("dp")
        }
    }

    @Test
    fun testElementOrderMaintained() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()

            assertThat(firstGenNodes).hasNames("group", "clip-path", "path")
        }
    }

    @Test
    fun testVectorAttributesWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootNode = output.firstChild

            // +1 for name
            assertThat(rootNode.attributes.length).isEqualTo(graphic.foreign.size + 1)
        }
    }

    @Test
    fun testPathMetadataWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstPathNode = output.firstChild.firstChild.firstChild

            assertThat(firstPathNode.attributes.getNamedItem("android:name")).hasValue("strike_thru_path")
        }
    }

    @Test
    fun testGroupChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val groupChildren =
                output.firstChild.firstChild.childNodes
                    .toList()

            assertThat(groupChildren).hasSameSizeAs((graphic.elements[0] as Group).elements)
        }
    }

    @Test
    fun testGroupWithTransform() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val transformGroup = output.firstChild.firstChild

            assertThat(transformGroup.attributes.getNamedItem("android:translateX")).hasValue("10")
            assertThat(transformGroup.attributes.getNamedItem("android:translateY")).hasValue("15")
        }
    }

    @Test
    fun testExtraWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode =
                output.firstChild.firstChild.childNodes
                    .item(1)

            assertThat(extraNode).hasName("bicycle")
        }
    }

    @Test
    fun testExtraChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode =
                output.firstChild.firstChild.childNodes
                    .item(1)

            assertThat(extraNode.firstChild).hasName("group")
        }
    }

    private fun ByteArrayOutputStream.toDocument(): Document =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(this.toByteArray()))

    companion object {
        val graphic =
            VectorDrawable(
                listOf(
                    Group(
                        listOf(
                            createPath(
                                CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(),
                                "strike_thru_path",
                            ),
                            Extra(
                                "bicycle",
                                listOf(Group(emptyList(), null, mutableMapOf(), Matrix3.IDENTITY)),
                                null,
                                mutableMapOf(),
                            ),
                        ),
                        "transform_group",
                        mutableMapOf(),
                        Matrix3.from(floatArrayOf(1f, 0f, 10f, 0f, 1f, 15f, 0f, 0f, 1f)),
                    ),
                    ClipPath(
                        listOf(
                            createPath(
                                CommandString(
                                    "M 0 0 L 24 0 L 24 24 L 0 24 L 0 0 Z M 4.54 1.73 L 3.27 3 L 3.27 3 L 4.54 1.73 Z",
                                ).toCommandList(),
                            ),
                        ),
                        null,
                        mutableMapOf(),
                    ),
                    createPath(
                        CommandString(
                            "M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 C 17 19.5 21.27 16.39 23 12 " +
                                "C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z M 12 17 C 9.24 17 7 14.76 7 12 " +
                                "C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 C 17 14.76 14.76 17 12 17 L 12 17 Z " +
                                "M 12 9 C 10.34 9 9 10.34 9 12 C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 " +
                                "C 15 10.34 13.66 9 12 9 L 12 9 Z",
                        ).toCommandList(),
                    ),
                ),
                "visibilitystrike",
                mutableMapOf(
                    "xmlns:android" to "http://schemas.android.com/apk/res/android",
                    "android:height" to "24dp",
                    "android:width" to "24dp",
                    "android:viewportHeight" to "24",
                    "android:viewportWidth" to "24",
                ),
            )
    }
}
