package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.Writer
import com.jzbrooks.guacamole.graphic.ClipPath
import com.jzbrooks.guacamole.graphic.Element
import com.jzbrooks.guacamole.graphic.Group
import com.jzbrooks.guacamole.graphic.Path
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class VectorDrawableWriter(override val options: Set<Writer.Option> = emptySet()) : Writer {

    fun write(graphic: VectorDrawable, stream: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("vector")
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
                    setAttribute("android:pathData", element.commands.joinToString(separator = ""))
                }
            }
            is Group -> {
                document.createElement("group").also {
                    for (child in element.elements) {
                        write(it, child, document)
                    }
                }
            }
            is ClipPath -> {
                document.createElement("clip-path").apply {
                    setAttribute("android:pathData", element.commands.joinToString(separator = ""))
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

        if (options.contains(Writer.Option.INDENT)) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }
}
