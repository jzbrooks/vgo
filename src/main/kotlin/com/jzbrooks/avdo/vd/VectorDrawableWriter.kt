package com.jzbrooks.avdo.vd

import com.jzbrooks.avdo.graphic.ClipPath
import com.jzbrooks.avdo.graphic.Group
import com.jzbrooks.avdo.graphic.Path
import org.w3c.dom.Document
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

fun write(graphic: VectorDrawable, outputStream: OutputStream) {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = builder.newDocument()

    val root = document.createElement("vector")
    root.setAttribute("xmlns:android", "https://schemas.android.com/apk/res/android")
    root.setAttribute("android:viewportWidth", graphic.viewBox.width.toString())
    root.setAttribute("android:viewportHeight", graphic.viewBox.height.toString())
    root.setAttribute("android:width", graphic.size.width.toString())
    root.setAttribute("android:height", graphic.size.height.toString())

    loop@ for (element in graphic.elements) {
        val node = when (element) {
            is Path -> {
                val pathElement = document.createElement("path")
                pathElement.setAttribute("android:pathData", element.data)
                pathElement
            }
            is Group -> {
                document.createElement("group")
            }
            is ClipPath -> {
                document.createElement("clip-path")
            }
            else -> continue@loop
        }
        for (item in element.metadata) {
            node.setAttribute(item.key, item.value)
        }
        root.appendChild(node)
    }

    for (item in graphic.metadata) {
        root.setAttribute(item.key, item.value)
    }

    document.appendChild(root)
    write(document, outputStream)
}

private fun write(document: Document, outputStream: OutputStream) {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    val source = DOMSource(document)
    val result = StreamResult(outputStream)
    transformer.transform(source, result)
}