package com.jzbrooks.vgo.vd

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasSameSizeAs
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
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

            // Top-level: a group (clip-path lives inside as a property) and a path.
            assertThat(firstGenNodes).hasNames("group", "path")

            val groupChildren =
                output.firstChild.firstChild.childNodes
                    .toList()
            // Group's clip-path is emitted first, then the original child elements.
            assertThat(groupChildren).hasNames("clip-path", "path", "bicycle")
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
            // Group emits its clip-path(s) first, then the original child elements;
            // the first path is at index 1.
            val firstPathNode =
                output.firstChild.firstChild.childNodes
                    .item(1)

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

            val group = graphic.elements[0] as Group
            // Output prepends a <clip-path> per Group.clipPaths entry to the group's elements.
            assertThat(groupChildren).hasSize(group.elements.size + group.clipPaths.size)
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
            // Group children layout: [clip-path, path("strike_thru_path"), bicycle].
            val extraNode =
                output.firstChild.firstChild.childNodes
                    .item(2)

            assertThat(extraNode).hasName("bicycle")
        }
    }

    @Test
    fun testLinearGradientFillWritten() {
        val gradient =
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

        val gradientGraphic =
            VectorDrawable(
                listOf(
                    createPath(
                        CommandString("M 0 0 L 24 0 L 24 24 L 0 24 Z").toCommandList(),
                        fill = gradient,
                    ),
                ),
                "gradient",
                mutableMapOf(
                    "xmlns:android" to "http://schemas.android.com/apk/res/android",
                    "android:viewportWidth" to "24",
                    "android:viewportHeight" to "24",
                ),
            )

        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(gradientGraphic, memoryStream)

            val output = memoryStream.toDocument()
            val root = output.firstChild

            assertThat(root.attributes.getNamedItem("xmlns:aapt")).hasValue("http://schemas.android.com/aapt")

            val pathNode = root.firstChild
            val aaptAttr = pathNode.childNodes.toList().single { it.nodeName == "aapt:attr" }
            assertThat(aaptAttr.attributes.getNamedItem("name")).hasValue("android:fillColor")

            val gradientNode = aaptAttr.firstChild
            assertThat(gradientNode).hasName("gradient")
            assertThat(gradientNode.attributes.getNamedItem("android:type")).hasValue("linear")
            assertThat(gradientNode.attributes.getNamedItem("android:startX").nodeValue).startsWith("0")
            assertThat(gradientNode.attributes.getNamedItem("android:endX").nodeValue).startsWith("24")

            val items = gradientNode.childNodes.toList().filter { it.nodeName == "item" }
            assertThat(items).transform("item count") { it.size }.isEqualTo(3)
            assertThat(
                items[0]
                    .attributes
                    .getNamedItem("android:color")
                    .nodeValue
                    .lowercase(),
            ).isEqualTo("#b125ea")
            assertThat(items[2].attributes.getNamedItem("android:offset").nodeValue).startsWith("1")
        }
    }

    @Test
    fun testSyntheticClipGroupElided() {
        // [path, syntheticClipGroup] at root: the writer should inline the
        // synthetic group's clip-paths and children directly into <vector>.
        val clip =
            ClipPath(
                listOf(createPath(CommandString("M 0 0 L 10 0 L 10 10 L 0 10 Z").toCommandList())),
            )
        val drawable =
            VectorDrawable(
                listOf(
                    createPath(CommandString("M 0 0 L 1 1 Z").toCommandList()),
                    Group(
                        elements = listOf(createPath(CommandString("M 2 2 L 3 3 Z").toCommandList())),
                        id = null,
                        foreign = mutableMapOf(),
                        transform = Matrix3.IDENTITY,
                        clipPaths = listOf(clip),
                    ),
                ),
                null,
                mutableMapOf("xmlns:android" to "http://schemas.android.com/apk/res/android"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(drawable, memoryStream)
            val output = memoryStream.toDocument()

            val rootChildren = output.firstChild.childNodes.toList()
            assertThat(rootChildren).hasNames("path", "clip-path", "path")
        }
    }

    @Test
    fun testNonLastClipOnlyGroupPreserved() {
        // The clip-only group is NOT last in its parent's elements, so eliding
        // would leak the clip onto the trailing sibling path. The <group>
        // wrapper must be preserved.
        val clip =
            ClipPath(
                listOf(createPath(CommandString("M 0 0 L 10 0 L 10 10 L 0 10 Z").toCommandList())),
            )
        val drawable =
            VectorDrawable(
                listOf(
                    Group(
                        elements = listOf(createPath(CommandString("M 2 2 L 3 3 Z").toCommandList())),
                        id = null,
                        foreign = mutableMapOf(),
                        transform = Matrix3.IDENTITY,
                        clipPaths = listOf(clip),
                    ),
                    createPath(CommandString("M 4 4 L 5 5 Z").toCommandList()),
                ),
                null,
                mutableMapOf("xmlns:android" to "http://schemas.android.com/apk/res/android"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(drawable, memoryStream)
            val output = memoryStream.toDocument()

            val rootChildren = output.firstChild.childNodes.toList()
            assertThat(rootChildren).hasNames("group", "path")
        }
    }

    @Test
    fun testNamedClipOnlyGroupNotElided() {
        // A non-null id blocks elision so the <group> wrapper survives.
        val clip =
            ClipPath(
                listOf(createPath(CommandString("M 0 0 L 10 0 L 10 10 L 0 10 Z").toCommandList())),
            )
        val drawable =
            VectorDrawable(
                listOf(
                    Group(
                        elements = listOf(createPath(CommandString("M 2 2 L 3 3 Z").toCommandList())),
                        id = "named_clip_group",
                        foreign = mutableMapOf(),
                        transform = Matrix3.IDENTITY,
                        clipPaths = listOf(clip),
                    ),
                ),
                null,
                mutableMapOf("xmlns:android" to "http://schemas.android.com/apk/res/android"),
            )

        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(drawable, memoryStream)
            val output = memoryStream.toDocument()

            val rootChildren = output.firstChild.childNodes.toList()
            assertThat(rootChildren).hasNames("group")
        }
    }

    @Test
    fun testExtraChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            // Group children layout: [clip-path, path("strike_thru_path"), bicycle].
            val extraNode =
                output.firstChild.firstChild.childNodes
                    .item(2)

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
                        elements =
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
                        id = "transform_group",
                        foreign = mutableMapOf(),
                        transform = Matrix3.from(floatArrayOf(1f, 0f, 10f, 0f, 1f, 15f, 0f, 0f, 1f)),
                        clipPaths =
                            listOf(
                                ClipPath(
                                    listOf(
                                        createPath(
                                            CommandString(
                                                "M 0 0 L 24 0 L 24 24 L 0 24 L 0 0 Z M 4.54 1.73 L 3.27 3 L 3.27 3 L 4.54 1.73 Z",
                                            ).toCommandList(),
                                        ),
                                    ),
                                ),
                            ),
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
