package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.svg.graphic.ClipPath
import org.w3c.dom.Document
import java.io.OutputStream
import java.util.Collections.emptySet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ScalableVectorGraphicWriter(
    override val options: Set<Writer.Option> = emptySet(),
) : Writer<ScalableVectorGraphic> {

    private val commandPrinter = ScalableVectorGraphicCommandPrinter(3)

    override fun write(graphic: ScalableVectorGraphic, stream: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("svg")
        for (item in graphic.attributes) {
            root.setAttribute(item.key, item.value)
        }
        document.appendChild(root)

        for (element in graphic.elements) {
            write(root, element, document)
        }

        write(document, stream)
    }

    private fun write(parent: org.w3c.dom.Element, element: Element, document: Document) {
        val node = when (element) {
            is Path -> {
                document.createElement("path").apply {
                    val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                    setAttribute("d", data)
                }
            }
            is Group -> {
                document.createElement("g").also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            is ClipPath -> {
                document.createElement("clipPath").also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            is Extra -> {
                document.createElement(element.name).also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            else -> null
        }

        if (node != null) {
            for (item in element.attributes) {
                node.setAttribute(item.key, item.value)
            }
            parent.appendChild(node)
        }
    }

    private fun write(document: Document, outputStream: OutputStream) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val indent = options.filterIsInstance<Writer.Option.Indent>().singleOrNull()?.columns
        if (indent != null) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString())
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }
}
