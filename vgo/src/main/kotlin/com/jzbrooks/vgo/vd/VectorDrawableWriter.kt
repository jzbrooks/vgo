package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.vd.graphic.ClipPath
import org.w3c.dom.Document
import java.io.OutputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Collections.emptySet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot

class VectorDrawableWriter(override val options: Set<Writer.Option> = emptySet()) : Writer<VectorDrawable> {

    private val commandPrinter = VectorDrawableCommandPrinter(3)

    private val formatter = DecimalFormat().apply {
        maximumFractionDigits = 2 // todo: parameterize?
        isDecimalSeparatorAlwaysShown = false
        roundingMode = RoundingMode.HALF_UP
    }

    override fun write(graphic: VectorDrawable, stream: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("vector")

        val elementName = graphic.id
        if (elementName != null) {
            root.setAttribute("android:name", elementName)
        }

        for (item in graphic.foreign) {
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
                    setAttribute("android:pathData", data)

                    if (element.fill.alpha != 0.toUByte()) {
                        val color = element.fill.toHexString(Color.HexFormat.ARGB)
                        setAttribute("android:fillColor", color)
                    }

                    if (element.stroke.alpha != 0.toUByte()) {
                        val color = element.stroke.toHexString(Color.HexFormat.ARGB)
                        setAttribute("android:strokeColor", color)
                    }

                    if (element.strokeWidth != 0f) {
                        setAttribute("android:strokeWidth", formatter.format(element.strokeWidth))
                    }
                }
            }
            is Group -> {
                document.createElement("group").also { node ->
                    // There's no reason to output the transforms if the
                    // value of the transform is referentially equal to the
                    // identity matrix constant
                    if (element.transform !== Matrix3.IDENTITY) {
                        writeTransforms(element, node)
                    }

                    for (child in element.elements) {
                        write(node, child, document)
                    }
                }
            }
            is ClipPath -> {
                document.createElement("clip-path").apply {
                    val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                    setAttribute("android:pathData", data)
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
            val elementName = element.id
            if (elementName != null) {
                node.setAttribute("android:name", elementName)
            }

            for ((key, value) in element.foreign) {
                node.setAttribute(key, value)
            }

            parent.appendChild(node)
        }
    }

    private fun writeTransforms(group: Group, node: org.w3c.dom.Element) {
        val a = group.transform[0, 0]
        val b = group.transform[0, 1]
        val c = group.transform[1, 0]
        val d = group.transform[1, 1]
        val e = group.transform[0, 2]
        val f = group.transform[1, 2]

        if (abs(e) >= 0.01f) {
            node.setAttribute("android:translateX", formatter.format(e))
        }

        if (abs(f) >= 0.01f) {
            node.setAttribute("android:translateY", formatter.format(f))
        }

        val scaleX = hypot(a, c)
        if (abs(scaleX) >= 1.01f) {
            node.setAttribute("android:scaleX", formatter.format(scaleX))
        }

        val scaleY = hypot(b, d)
        if (abs(scaleY) != 1.01f) {
            node.setAttribute("android:scaleY", formatter.format(scaleY))
        }

        val rotation = atan(c / d)
        if (abs(rotation) >= 0.01f) {
            node.setAttribute("android:rotation", formatter.format(rotation))
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
