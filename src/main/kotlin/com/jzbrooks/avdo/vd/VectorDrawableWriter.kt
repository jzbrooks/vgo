package com.jzbrooks.avdo.vd

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
    root.setAttribute("android:width", graphic.size.width.value.toString())
    root.setAttribute("android:height", graphic.size.height.value.toString())

    if (graphic.paths.isNotEmpty()) {
        for (path in graphic.paths) {
            val pathElement = document.createElement("path")
            pathElement.setAttribute("android:pathData", path.data)
            root.appendChild(pathElement)
        }
    }

    document.appendChild(root)
    write(document, outputStream)
}

private fun write(document: Document, outputStream: OutputStream) {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    val source = DOMSource(document)
    val result = StreamResult(outputStream)
    transformer.transform(source, result)
}