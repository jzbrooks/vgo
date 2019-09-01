package com.jzbrooks.avdo

import com.jzbrooks.avdo.graphic.*
import com.jzbrooks.avdo.util.xml.toList
import com.jzbrooks.avdo.vd.VectorDrawable
import com.jzbrooks.avdo.vd.write
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assert
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorDrawableWriterTests {
    @Test
    fun testRootElementIsVector() {
        ByteArrayOutputStream().use {
            write(graphic, it)

            val output = it.toDocument()
            assertEquals("vector", output.childNodes.item(0).nodeName)
        }
    }

    @Test
    fun testIsViewboxWritten() {
        ByteArrayOutputStream().use {
            write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertEquals("24", rootAttributes.getNamedItem("android:viewportWidth").nodeValue)
            assertEquals("24", rootAttributes.getNamedItem("android:viewportHeight").nodeValue)
        }
    }

    @Test
    fun testIsSizeWritten() {
        ByteArrayOutputStream().use {
            write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertTrue(rootAttributes.getNamedItem("android:width").nodeValue.startsWith("24"))
            assertTrue(rootAttributes.getNamedItem("android:height").nodeValue.startsWith("24"))
        }
    }

    @Test
    fun testPathsWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.childNodes.item(0).childNodes.toList()

            assertEquals(2, firstGenNodes.count { it.nodeName == "path" })
        }
    }

    @Test
    fun testUnitsWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertTrue(rootAttributes.getNamedItem("android:height").nodeValue.endsWith("dp"))
            assertTrue(rootAttributes.getNamedItem("android:width").nodeValue.endsWith("dp"))
        }
    }

    @Test
    fun testElementOrderMaintained() {
        ByteArrayOutputStream().use {  memoryStream ->
            write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.childNodes.item(0).childNodes.toList()

            assertEquals("path", firstGenNodes[0].nodeName)
            assertEquals("clip-path", firstGenNodes[1].nodeName)
            assertEquals("path", firstGenNodes[2].nodeName)
        }
    }

    @Test
    fun testVectorMetadataWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootNode = output.childNodes.item(0)

            assertEquals("visibilitystrike", rootNode.attributes.getNamedItem("android:name").nodeValue)
        }
    }

    @Test
    fun testPathMetadataWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstPathNode = output.childNodes.item(0).childNodes.item(0)

            assertEquals("strike_thru_path", firstPathNode.attributes.getNamedItem("android:name").nodeValue)
        }
    }

    private fun ByteArrayOutputStream.toDocument(): Document {
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(this.toByteArray()))
    }

    companion object {
        val graphic = VectorDrawable(
                listOf(
                        Path("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z", 1, mapOf("android:name" to "strike_thru_path")),
                        ClipPath("M 0 0 L 24 0 L 24 24 L 0 24 L 0 0 Z M 4.54 1.73 L 3.27 3 L 3.27 3 L 4.54 1.73 Z"),
                        Path("M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 L 12 9 Z", 1)
                ),
                Size(Dimension(24, Dimension.Unit.Dp), Dimension(24, Dimension.Unit.Dp)),
                mapOf(
                        "android:name" to "visibilitystrike"
                )
        )
    }
}