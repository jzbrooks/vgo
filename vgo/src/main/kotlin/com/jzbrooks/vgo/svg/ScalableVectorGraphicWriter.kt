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

    override fun write(
        graphic: ScalableVectorGraphic,
        stream: OutputStream,
    ) {
        val document = graphic.toDocument(commandPrinter)
        write(document, stream)
    }

    fun write(
        document: Document,
        outputStream: OutputStream,
    ) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        val indent = options.filterIsInstance<Writer.Option.Indent>().singleOrNull()?.columns
        if (indent != null) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString())
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", indent.toString())
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }
}

private data class InheritedStyle(
    val fill: Color = Colors.BLACK,
    val fillRule: Path.FillRule = Path.FillRule.NON_ZERO,
    val stroke: Color = Colors.TRANSPARENT,
    val strokeWidth: Float = 1f,
    val strokeLineCap: Path.LineCap = Path.LineCap.BUTT,
    val strokeLineJoin: Path.LineJoin = Path.LineJoin.MITER,
    val strokeMiterLimit: Float = 4f,
)

private fun Map<String, String>.withoutImpliedPresentationAttrs(inherited: InheritedStyle): Map<String, String> =
    filter { (key, _) ->
        when (key) {
            "fill" -> (extractColor("fill", inherited.fill) ?: inherited.fill) != inherited.fill
            "fill-rule" -> (extractFillRule("fill-rule") ?: inherited.fillRule) != inherited.fillRule
            "stroke" -> (extractColor("stroke", inherited.stroke) ?: inherited.stroke) != inherited.stroke
            "stroke-width" -> this["stroke-width"]?.toFloatOrNull() != inherited.strokeWidth
            "stroke-linecap" -> (extractLineCap("stroke-linecap") ?: inherited.strokeLineCap) != inherited.strokeLineCap
            "stroke-linejoin" -> (extractLineJoin("stroke-linejoin") ?: inherited.strokeLineJoin) != inherited.strokeLineJoin
            "stroke-miterlimit" -> this["stroke-miterlimit"]?.toFloatOrNull() != inherited.strokeMiterLimit
            else -> true
        }
    }

private fun Map<String, String>.toChildInheritedStyle(current: InheritedStyle): InheritedStyle {
    val styleAttrs = this["style"]?.parseStyleAttribute() ?: emptyMap()
    val merged = this + styleAttrs
    return InheritedStyle(
        fill = merged.extractColor("fill", current.fill) ?: current.fill,
        fillRule = merged.extractFillRule("fill-rule") ?: current.fillRule,
        stroke = merged.extractColor("stroke", current.stroke) ?: current.stroke,
        strokeWidth = merged["stroke-width"]?.toFloatOrNull() ?: current.strokeWidth,
        strokeLineCap = merged.extractLineCap("stroke-linecap") ?: current.strokeLineCap,
        strokeLineJoin = merged.extractLineJoin("stroke-linejoin") ?: current.strokeLineJoin,
        strokeMiterLimit = merged["stroke-miterlimit"]?.toFloatOrNull() ?: current.strokeMiterLimit,
    )
}

fun ScalableVectorGraphic.toDocument(commandPrinter: ScalableVectorGraphicCommandPrinter): Document {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = builder.newDocument()

    val root = document.createElement("svg")
    val elementName = id
    if (elementName != null) {
        root.setAttribute("id", elementName)
    }
    val writtenRootForeign = foreign.withoutImpliedPresentationAttrs(InheritedStyle())
    for (item in writtenRootForeign) {
        root.setAttribute(item.key, item.value)
    }
    document.appendChild(root)

    val inherited = foreign.toChildInheritedStyle(InheritedStyle())
    for (element in elements) {
        document.createChildElement(commandPrinter, root, element, inherited)
    }

    return document
}

private fun Document.createChildElement(
    commandPrinter: ScalableVectorGraphicCommandPrinter,
    parent: org.w3c.dom.Element,
    element: Element,
    inherited: InheritedStyle = InheritedStyle(),
) {
    val writtenForeign = element.foreign.withoutImpliedPresentationAttrs(inherited)
    val node =
        when (element) {
            is Path -> {
                createElement("path").apply {
                    val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                    setAttribute("d", data)

                    if (element.fill != inherited.fill) {
                        if (element.fill.alpha == 0.toUByte()) {
                            setAttribute("fill", "none")
                        } else {
                            val color = Colors.NAMES_BY_COLORS[element.fill] ?: element.fill.toHexString(Color.HexFormat.RGBA)
                            setAttribute("fill", color)
                        }
                    }

                    if (element.fillRule != inherited.fillRule) {
                        val fillRule =
                            when (element.fillRule) {
                                Path.FillRule.EVEN_ODD -> "evenodd"
                                Path.FillRule.NON_ZERO -> "nonzero"
                            }
                        setAttribute("fill-rule", fillRule)
                    }

                    if (element.stroke != inherited.stroke) {
                        if (element.stroke.alpha == 0.toUByte()) {
                            setAttribute("stroke", "none")
                        } else {
                            val color = Colors.NAMES_BY_COLORS[element.stroke] ?: element.stroke.toHexString(Color.HexFormat.RGBA)
                            setAttribute("stroke", color)
                        }
                    }

                    if (element.strokeWidth != inherited.strokeWidth) {
                        setAttribute("stroke-width", commandPrinter.formatter.format(element.strokeWidth))
                    }

                    if (element.strokeLineCap != inherited.strokeLineCap) {
                        val lineCap =
                            when (element.strokeLineCap) {
                                Path.LineCap.SQUARE -> "square"
                                Path.LineCap.ROUND -> "round"
                                Path.LineCap.BUTT -> "butt"
                            }
                        setAttribute("stroke-linecap", lineCap)
                    }

                    if (element.strokeLineJoin != inherited.strokeLineJoin) {
                        val lineJoin =
                            when (element.strokeLineJoin) {
                                Path.LineJoin.ROUND -> "round"
                                Path.LineJoin.BEVEL -> "bevel"
                                Path.LineJoin.MITER_CLIP -> "miter-clip"
                                Path.LineJoin.ARCS -> "arcs"
                                Path.LineJoin.MITER -> "miter"
                            }
                        setAttribute("stroke-linejoin", lineJoin)
                    }

                    if (element.strokeMiterLimit != inherited.strokeMiterLimit) {
                        setAttribute("stroke-miterlimit", commandPrinter.formatter.format(element.strokeMiterLimit))
                    }
                }
            }

            is Group -> {
                createElement("g").also { node ->
                    if (!element.transform.contentsEqual(Matrix3.IDENTITY)) {
                        val matrix = element.transform
                        val matrixElements =
                            listOf(
                                commandPrinter.formatter.format(matrix[0, 0]),
                                commandPrinter.formatter.format(matrix[1, 0]),
                                commandPrinter.formatter.format(matrix[0, 1]),
                                commandPrinter.formatter.format(matrix[1, 1]),
                                commandPrinter.formatter.format(matrix[0, 2]),
                                commandPrinter.formatter.format(matrix[1, 2]),
                            ).joinToString(separator = ",")

                        node.setAttribute("transform", "matrix($matrixElements)")
                    }

                    val childInherited = writtenForeign.toChildInheritedStyle(inherited)
                    for (child in element.elements) {
                        createChildElement(commandPrinter, node, child, childInherited)
                    }
                }
            }

            is ClipPath -> {
                createElement("clipPath").also {
                    val childInherited = writtenForeign.toChildInheritedStyle(inherited)
                    for (child in element.elements) {
                        createChildElement(commandPrinter, it, child, childInherited)
                    }
                }
            }

            is Extra -> {
                createElement(element.name).also {
                    val childInherited = writtenForeign.toChildInheritedStyle(inherited)
                    for (child in element.elements) {
                        createChildElement(commandPrinter, it, child, childInherited)
                    }
                }
            }

            else -> {
                null
            }
        }

    if (node != null) {
        val elementName = element.id
        if (elementName != null) {
            node.setAttribute("id", elementName)
        }
        for (item in writtenForeign) {
            node.setAttribute(item.key, item.value)
        }
        parent.appendChild(node)
    }
}
