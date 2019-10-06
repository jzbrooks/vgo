package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.jzbrooks.guacamole.assertk.extensions.hasName
import com.jzbrooks.guacamole.assertk.extensions.hasValue
import com.jzbrooks.guacamole.core.graphic.ClipPath
import com.jzbrooks.guacamole.core.graphic.Extra
import com.jzbrooks.guacamole.core.graphic.Group
import com.jzbrooks.guacamole.core.graphic.Path
import com.jzbrooks.guacamole.core.graphic.command.CommandString
import com.jzbrooks.guacamole.core.util.xml.toList
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test

class VectorDrawableWriterTests {
    @Test
    fun testRootElementIsVector() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            assertThat(output.childNodes.item(0)).hasName("vector")
        }
    }

    @Test
    fun testIsViewboxWritten() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertThat(rootAttributes.getNamedItem("android:viewportWidth")).hasValue("24")
            assertThat(rootAttributes.getNamedItem("android:viewportHeight")).hasValue("24")
        }
    }

    @Test
    fun testIsSizeWritten() {
        ByteArrayOutputStream().use {
            VectorDrawableWriter().write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertThat(rootAttributes.getNamedItem("android:width").nodeValue).startsWith("24")
            assertThat(rootAttributes.getNamedItem("android:height").nodeValue).startsWith("24")
        }
    }

    @Test
    fun testPathsWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.childNodes.item(0).childNodes.toList()

            assertThat(firstGenNodes)
                    .transform { it.count { item -> item.nodeName == "path" } }
                    .isEqualTo(2)
        }
    }
    
    @Test
    fun testUnitsWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootAttributes = output.childNodes.item(0).attributes

            assertThat(rootAttributes.getNamedItem("android:height").nodeValue).endsWith("dp")
            assertThat(rootAttributes.getNamedItem("android:width").nodeValue).endsWith("dp")
        }
    }

    @Test
    fun testElementOrderMaintained() {
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.childNodes.item(0).childNodes.toList()

            assertThat(firstGenNodes[0]).hasName("path")
            assertThat(firstGenNodes[1]).hasName("bicycle")
            assertThat(firstGenNodes[2]).hasName("clip-path")
            assertThat(firstGenNodes[3]).hasName("path")
        }
    }

    @Test
    fun testVectorAttributesWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootNode = output.childNodes.item(0)

            assertThat(rootNode.attributes.length).isEqualTo(attributes.size)
        }
    }

    @Test
    fun testPathMetadataWritten() {
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstPathNode = output.childNodes.item(0).childNodes.item(0)

            assertThat(firstPathNode.attributes.getNamedItem("android:name")).hasValue("strike_thru_path")
        }
    }

    @Test
    fun testGroupChildrenWritten() {
        val graphicWithGroup = VectorDrawable(listOf(Group(graphic.elements)), attributes)
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val groupNode = output.childNodes.item(0).childNodes.item(0)

            assertThat(groupNode.childNodes.length).isEqualTo(graphic.elements.size)
        }
    }

    @Test
    fun testExtraWritten() {
        val graphicWithGroup = VectorDrawable(graphic.elements, attributes)
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode = output.childNodes.item(0).childNodes.item(1)

            assertThat(extraNode.nodeName).isEqualTo("bicycle")
        }
    }

    @Test
    fun testExtraChildrenWritten() {
        val graphicWithGroup = VectorDrawable(graphic.elements, attributes)
        ByteArrayOutputStream().use {  memoryStream ->
            VectorDrawableWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode = output.childNodes.item(0).childNodes.item(1)

            assertThat(extraNode.childNodes.item(0)).hasName("group")
        }
    }

    private fun ByteArrayOutputStream.toDocument(): Document {
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(this.toByteArray()))
    }

    companion object {
        val attributes = mapOf(
                "xmlns:android" to "http://schemas.android.com/apk/res/android",
                "android:name" to "visibilitystrike",
                "android:height" to "24dp",
                "android:width" to "24dp",
                "android:viewportHeight" to "24",
                "android:viewportWidth" to "24"
        )

        val graphic = VectorDrawable(
                listOf(
                        Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), mapOf("android:name" to "strike_thru_path")),
                        Extra("bicycle", listOf(Group(emptyList())), emptyMap()),
                        ClipPath(CommandString("M 0 0 L 24 0 L 24 24 L 0 24 L 0 0 Z M 4.54 1.73 L 3.27 3 L 3.27 3 L 4.54 1.73 Z").toCommandList()),
                        Path(CommandString("M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 L 12 9 Z").toCommandList())
                ),
                attributes
        )
    }
}