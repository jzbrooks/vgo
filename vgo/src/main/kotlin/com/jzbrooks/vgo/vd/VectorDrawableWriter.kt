package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.util.math.Matrix3
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

    private val formatter =
        DecimalFormat().apply {
            maximumFractionDigits = 2 // todo: parameterize?
            minimumIntegerDigits = 0
            isDecimalSeparatorAlwaysShown = false
            roundingMode = RoundingMode.HALF_UP
        }

    override fun write(
        graphic: VectorDrawable,
        stream: OutputStream,
    ) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("vector")

        val elementName = graphic.id
        if (elementName != null) {
            root.setAttribute("android:name", elementName)
        }

        for (item in graphic.foreign.filter { (k, v) -> DEFAULT_ROOT_ATTRIBUTES[k] != v }) {
            root.setAttribute(item.key, item.value)
        }
        document.appendChild(root)

        for (element in graphic.elements) {
            write(root, element, document)
        }

        write(document, stream)
    }

    private fun write(
        parent: org.w3c.dom.Element,
        element: Element,
        document: Document,
    ) {
        val node =
            when (element) {
                is Path -> {
                    document.createElement("path").apply {
                        val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                        setAttribute("android:pathData", data)

                        if (element.fill.alpha != 0.toUByte()) {
                            val color = element.fill.toHexString(Color.HexFormat.ARGB)
                            setAttribute("android:fillColor", color)
                        }

                        if (element.fillRule != Path.FillRule.NON_ZERO) {
                            val fillType =
                                when (element.fillRule) {
                                    Path.FillRule.EVEN_ODD -> "evenOdd"
                                    Path.FillRule.NON_ZERO -> throw IllegalStateException(
                                        "Default fill type ('nonZero') should never be written",
                                    )
                                }
                            setAttribute("android:fillType", fillType)
                        }

                        if (element.stroke.alpha != 0.toUByte()) {
                            val color = element.stroke.toHexString(Color.HexFormat.ARGB)
                            setAttribute("android:strokeColor", color)
                        }

                        if (element.strokeWidth != 0f) {
                            setAttribute("android:strokeWidth", formatter.format(element.strokeWidth))
                        }

                        if (element.strokeLineCap != Path.LineCap.BUTT) {
                            val lineCap =
                                when (element.strokeLineCap) {
                                    Path.LineCap.SQUARE -> "square"
                                    Path.LineCap.ROUND -> "round"
                                    Path.LineCap.BUTT -> throw IllegalStateException(
                                        "Default linecap ('butt') shouldn't ever be written.",
                                    )
                                }
                            setAttribute("android:strokeLineCap", lineCap)
                        }

                        if (element.strokeLineJoin != Path.LineJoin.MITER) {
                            val lineJoin =
                                when (val lineJoin = element.strokeLineJoin) {
                                    Path.LineJoin.ROUND -> "round"
                                    Path.LineJoin.BEVEL -> "bevel"
                                    Path.LineJoin.MITER -> throw IllegalStateException(
                                        "Default linejoin ('miter') shouldn't ever be written.",
                                    )
                                    Path.LineJoin.MITER_CLIP, Path.LineJoin.ARCS -> throw IllegalStateException(
                                        "VectorDrawable does not support line join: $lineJoin",
                                    )
                                }
                            setAttribute("android:strokeLineJoin", lineJoin)
                        }

                        if (element.strokeMiterLimit != 4f) {
                            setAttribute("android:strokeLineJoin", formatter.format(element.strokeMiterLimit))
                        }
                    }
                }
                is Group -> {
                    document.createElement("group").also { node ->
                        // There's no reason to output the transforms if the
                        // value of the transform is referentially equal to the
                        // identity matrix constant
                        if (!element.transform.contentsEqual(Matrix3.IDENTITY)) {
                            writeTransforms(element, node)
                        }

                        for (child in element.elements) {
                            write(node, child, document)
                        }
                    }
                }
                is ClipPath -> {
                    document.createElement("clip-path").apply {
                        val data =
                            (element.elements[0] as Path).commands
                                .joinToString(separator = "", transform = commandPrinter::print)

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

            for ((key, value) in element.foreign.filter { (k, v) -> DEFAULT_PATH_ATTRIBUTES[k] != v }) {
                node.setAttribute(key, value)
            }

            parent.appendChild(node)
        }
    }

    private fun writeTransforms(
        group: Group,
        node: org.w3c.dom.Element,
    ) {
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
        if (abs(scaleY) >= 1.01f) {
            node.setAttribute("android:scaleY", formatter.format(scaleY))
        }

        val rotation = atan(c / d)
        if (abs(rotation) >= 0.01f) {
            node.setAttribute("android:rotation", formatter.format(rotation))
        }
    }

    private fun write(
        document: Document,
        outputStream: OutputStream,
    ) {
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

    companion object {
        private val DEFAULT_ROOT_ATTRIBUTES =
            mapOf(
                "android:alpha" to "1.0",
                "android:autoMirrored" to "false",
                "android:tintMode" to "src_in",
            )

        private val DEFAULT_PATH_ATTRIBUTES =
            mapOf(
                "android:trimPathStart" to "0",
                "android:trimPathEnd" to "1",
                "android:trimPathOffset" to "0",
            )
    }
}
