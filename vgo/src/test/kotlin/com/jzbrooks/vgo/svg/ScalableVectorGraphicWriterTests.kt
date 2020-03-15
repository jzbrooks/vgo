package com.jzbrooks.vgo.svg

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.vgo.assertk.extensions.hasName
import com.jzbrooks.vgo.assertk.extensions.hasNames
import com.jzbrooks.vgo.assertk.extensions.hasValue
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.util.xml.toList
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory

class ScalableVectorGraphicWriterTests {
    @Test
    fun testRootElementIsSvg() {
        ByteArrayOutputStream().use {
            ScalableVectorGraphicWriter().write(graphic, it)

            val output = it.toDocument()
            assertThat(output.firstChild).hasName("svg")
        }
    }

    @Test
    fun testIsViewboxWritten() {
        ByteArrayOutputStream().use {
            ScalableVectorGraphicWriter().write(graphic, it)

            val output = it.toDocument()
            val rootAttributes = output.firstChild.attributes

            assertThat(rootAttributes.getNamedItem("viewbox").nodeValue).isEqualTo("0 0 100 100")
        }
    }

    @Test
    fun testPathsWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()

            assertThat(firstGenNodes)
                    .transform { it.count { item -> item.nodeName == "path" } }
                    .isEqualTo(2)
        }
    }

    @Test
    fun testElementOrderMaintained() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstGenNodes = output.firstChild.childNodes.toList()

            assertThat(firstGenNodes).hasNames("path", "bicycle", "path")
        }
    }

    @Test
    fun testVectorAttributesWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val rootNode = output.firstChild

            assertThat(rootNode.attributes.length).isEqualTo(attributes.size)
        }
    }

    @Test
    fun testPathMetadataWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphic, memoryStream)

            val output = memoryStream.toDocument()
            val firstPathNode = output.firstChild.firstChild

            assertThat(firstPathNode.attributes.getNamedItem("id")).hasValue("strike_thru_path")
        }
    }

    @Test
    fun testGroupChildrenWritten() {
        val graphicWithGroup = ScalableVectorGraphic(listOf(Group(graphic.elements)), attributes)
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val groupNode = output.firstChild.firstChild

            assertThat(groupNode.childNodes.length).isEqualTo(graphic.elements.size)
        }
    }

    @Test
    fun testExtraWritten() {
        val graphicWithGroup = ScalableVectorGraphic(graphic.elements, attributes)
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode = output.firstChild.childNodes.item(1)

            assertThat(extraNode.nodeName).isEqualTo("bicycle")
        }
    }

    @Test
    fun testExtraChildrenWritten() {
        val graphicWithGroup = ScalableVectorGraphic(graphic.elements, attributes)
        ByteArrayOutputStream().use { memoryStream ->
            ScalableVectorGraphicWriter().write(graphicWithGroup, memoryStream)

            val output = memoryStream.toDocument()
            val extraNode = output.firstChild.childNodes.item(1)

            assertThat(extraNode.firstChild).hasName("g")
        }
    }

    private fun ByteArrayOutputStream.toDocument(): Document {
        return DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(this.toByteArray()))
    }

    companion object {
        val attributes = mutableMapOf(
                "xmlns" to "http://www.w3.org/2000/svg",
                "viewbox" to "0 0 100 100"
        )

        val graphic = ScalableVectorGraphic(
                listOf(
                        Path(CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(), mutableMapOf("id" to "strike_thru_path")),
                        Extra("bicycle", listOf(Group(emptyList())), mutableMapOf()),
                        Path(CommandString("M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 L 12 9 Z").toCommandList())
                ),
                attributes
        )
    }
}