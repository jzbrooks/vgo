package com.jzbrooks.vgo.vd

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Gradient
import com.jzbrooks.vgo.core.HexFormat
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.Paint
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.Writer
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
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
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot

class VectorDrawableWriter(
    override val options: Set<Writer.Option> = emptySet(),
    private val commandPrinter: VectorDrawableCommandPrinter = VectorDrawableCommandPrinter(3),
) : Writer<VectorDrawable> {
    override fun write(
        graphic: VectorDrawable,
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
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }
}

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

fun VectorDrawable.toDocument(commandPrinter: VectorDrawableCommandPrinter): Document {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = builder.newDocument()

    val root = document.createElement("vector")

    val elementName = id
    if (elementName != null) {
        root.setAttribute("android:name", elementName)
    }

    for (item in foreign.filter { (k, v) -> DEFAULT_ROOT_ATTRIBUTES[k] != v }) {
        root.setAttribute(item.key, item.value)
    }

    if (containsGradientPaint() && !root.hasAttribute("xmlns:aapt")) {
        root.setAttribute("xmlns:aapt", "http://schemas.android.com/aapt")
    }

    document.appendChild(root)

    for (element in elements) {
        document.createChildElement(commandPrinter, root, element)
    }

    return document
}

private fun ContainerElement.containsGradientPaint(): Boolean =
    elements.any {
        when (it) {
            is Path -> it.fill is Gradient || it.stroke is Gradient
            is ContainerElement -> it.containsGradientPaint()
            else -> false
        }
    }

private fun Document.createChildElement(
    commandPrinter: VectorDrawableCommandPrinter,
    parent: org.w3c.dom.Element,
    element: Element,
) {
    val node =
        when (element) {
            is Path -> {
                createElement("path").apply {
                    val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                    setAttribute("android:pathData", data)

                    writePaintAttribute(commandPrinter, "android:fillColor", element.fill)

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

                    writePaintAttribute(commandPrinter, "android:strokeColor", element.stroke)

                    if (element.strokeWidth != 0f) {
                        setAttribute("android:strokeWidth", commandPrinter.formatter.format(element.strokeWidth))
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
                        setAttribute("android:strokeLineJoin", commandPrinter.formatter.format(element.strokeMiterLimit))
                    }
                }
            }

            is Group -> {
                createElement("group").also { node ->
                    // There's no reason to output the transforms if the
                    // value of the transform is referentially equal to the
                    // identity matrix constant
                    if (!element.transform.contentsEqual(Matrix3.IDENTITY)) {
                        writeTransforms(commandPrinter, element, node)
                    }

                    for (child in element.elements) {
                        createChildElement(commandPrinter, node, child)
                    }
                }
            }

            is ClipPath -> {
                createElement("clip-path").apply {
                    val data =
                        (element.elements[0] as Path)
                            .commands
                            .joinToString(separator = "", transform = commandPrinter::print)

                    setAttribute("android:pathData", data)
                }
            }

            is Extra -> {
                createElement(element.name).also {
                    for (child in element.elements) {
                        createChildElement(commandPrinter, it, child)
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
            node.setAttribute("android:name", elementName)
        }

        for ((key, value) in element.foreign.filter { (k, v) -> DEFAULT_PATH_ATTRIBUTES[k] != v }) {
            node.setAttribute(key, value)
        }

        parent.appendChild(node)
    }
}

private fun writeTransforms(
    commandPrinter: VectorDrawableCommandPrinter,
    group: Group,
    node: org.w3c.dom.Element,
) {
    val formatter = commandPrinter.formatter

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

private fun org.w3c.dom.Element.writePaintAttribute(
    commandPrinter: VectorDrawableCommandPrinter,
    attrName: String,
    paint: Paint,
) {
    when (paint) {
        is Color -> {
            if (paint.alpha != 0.toUByte()) {
                setAttribute(attrName, paint.toHexString(HexFormat.ARGB))
            }
        }

        is Gradient -> {
            appendChild(buildGradientAaptAttr(commandPrinter, ownerDocument, attrName, paint))
        }
    }
}

private fun buildGradientAaptAttr(
    commandPrinter: VectorDrawableCommandPrinter,
    document: Document,
    attrName: String,
    paint: Gradient,
): org.w3c.dom.Element {
    val formatter = commandPrinter.formatter
    val aaptAttr = document.createElement("aapt:attr")
    aaptAttr.setAttribute("name", attrName)

    val gradient = document.createElement("gradient")
    when (paint) {
        is LinearGradient -> {
            gradient.setAttribute("android:startX", formatter.format(paint.startX))
            gradient.setAttribute("android:startY", formatter.format(paint.startY))
            gradient.setAttribute("android:endX", formatter.format(paint.endX))
            gradient.setAttribute("android:endY", formatter.format(paint.endY))
            gradient.setAttribute("android:type", "linear")
            paint.tileMode.toAaptString()?.let { gradient.setAttribute("android:tileMode", it) }
        }

        is RadialGradient -> {
            gradient.setAttribute("android:centerX", formatter.format(paint.centerX))
            gradient.setAttribute("android:centerY", formatter.format(paint.centerY))
            gradient.setAttribute("android:gradientRadius", formatter.format(paint.radius))
            gradient.setAttribute("android:type", "radial")
            paint.tileMode.toAaptString()?.let { gradient.setAttribute("android:tileMode", it) }
        }

        is SweepGradient -> {
            gradient.setAttribute("android:centerX", formatter.format(paint.centerX))
            gradient.setAttribute("android:centerY", formatter.format(paint.centerY))
            gradient.setAttribute("android:type", "sweep")
        }
    }

    for (stop in paint.stops) {
        val item = document.createElement("item")
        item.setAttribute("android:offset", formatter.format(stop.offset))
        item.setAttribute("android:color", stop.color.toHexString(HexFormat.ARGB))
        gradient.appendChild(item)
    }

    aaptAttr.appendChild(gradient)
    return aaptAttr
}

private fun TileMode.toAaptString(): String? =
    when (this) {
        TileMode.CLAMP -> null
        TileMode.REPEAT -> "repeat"
        TileMode.MIRROR -> "mirror"
    }
