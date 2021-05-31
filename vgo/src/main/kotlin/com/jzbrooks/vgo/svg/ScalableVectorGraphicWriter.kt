package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
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

class ScalableVectorGraphicWriter(
    override val options: Set<Writer.Option> = emptySet(),
) : Writer<ScalableVectorGraphic> {

    private val commandPrinter = ScalableVectorGraphicCommandPrinter(3)

    private val formatter = DecimalFormat().apply {
        maximumFractionDigits = 2 // todo: parameterize?
        isDecimalSeparatorAlwaysShown = false
        roundingMode = RoundingMode.HALF_UP
    }

    override fun write(graphic: ScalableVectorGraphic, stream: OutputStream) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("svg")
        val elementName = graphic.id
        if (elementName != null) {
            root.setAttribute("id", elementName)
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
                    setAttribute("d", data)

                    if (element.fill != Colors.BLACK) {
                        if (element.fill.alpha == 0.toUByte()) {
                            setAttribute("fill", "none")
                        } else {
                            val color = Colors.NAMES_BY_COLORS[element.fill] ?: element.fill.toHexString(Color.HexFormat.RGBA)
                            setAttribute("fill", color)
                        }
                    }

                    if (element.fillRule != Path.FillRule.NON_ZERO) {
                        val fillRule = when (element.fillRule) {
                            Path.FillRule.EVEN_ODD -> "evenodd"
                            Path.FillRule.NON_ZERO -> throw IllegalStateException(
                                "Default fill rule ('nonzero') should never be written"
                            )
                        }
                        setAttribute("fill-rule", fillRule)
                    }

                    if (element.stroke.alpha != 0.toUByte()) {
                        val color = Colors.NAMES_BY_COLORS[element.stroke] ?: element.stroke.toHexString(Color.HexFormat.RGBA)
                        setAttribute("stroke", color)
                    }

                    if (element.strokeWidth != 1f) {
                        setAttribute("stroke-width", formatter.format(element.strokeWidth))
                    }

                    if (element.strokeLineCap != Path.LineCap.BUTT) {
                        val lineCap = when (element.strokeLineCap) {
                            Path.LineCap.SQUARE -> "square"
                            Path.LineCap.ROUND -> "round"
                            else -> throw IllegalStateException("Default linecap ('butt') shouldn't ever be written.")
                        }
                        setAttribute("stroke-linecap", lineCap)
                    }

                    if (element.strokeLineJoin != Path.LineJoin.MITER) {
                        val lineJoin = when (element.strokeLineJoin) {
                            Path.LineJoin.ROUND -> "round"
                            Path.LineJoin.BEVEL -> "bevel"
                            Path.LineJoin.MITER_CLIP -> "miter-clip"
                            Path.LineJoin.ARCS -> "arcs"
                            Path.LineJoin.MITER -> throw IllegalStateException(
                                "Default linejoin ('miter') shouldn't ever be written."
                            )
                        }
                        setAttribute("stroke-linejoin", lineJoin)
                    }

                    if (element.strokeMiterLimit != 4f) {
                        setAttribute("stroke-miterlimit", formatter.format(element.strokeMiterLimit))
                    }
                }
            }
            is Group -> {
                document.createElement("g").also { node ->
                    if (!element.transform.contentsEqual(Matrix3.IDENTITY)) {
                        val matrix = element.transform
                        val matrixElements = listOf(
                            formatter.format(matrix[0, 0]),
                            formatter.format(matrix[1, 0]),
                            formatter.format(matrix[0, 1]),
                            formatter.format(matrix[1, 1]),
                            formatter.format(matrix[0, 2]),
                            formatter.format(matrix[1, 2]),
                        ).joinToString(separator = ",")

                        node.setAttribute("transform", "matrix($matrixElements)")
                    }

                    for (child in element.elements) {
                        write(node, child, document)
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
            val elementName = element.id
            if (elementName != null) {
                node.setAttribute("id", elementName)
            }
            for (item in element.foreign) {
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
