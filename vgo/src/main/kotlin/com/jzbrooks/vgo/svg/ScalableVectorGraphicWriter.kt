package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.Gradient
import com.jzbrooks.vgo.core.HexFormat
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.SweepGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.PaintedElement
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.util.math.Matrix3
import org.w3c.dom.Document
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class ScalableVectorGraphicWriter(
    private val indent: Int = 0,
    private val commandPrinter: ScalableVectorGraphicCommandPrinter = ScalableVectorGraphicCommandPrinter(3),
) {
    fun write(
        graphic: ScalableVectorGraphic,
        stream: OutputStream,
    ) {
        val document = createDocument(graphic)
        write(document, stream)
    }

    fun write(
        document: Document,
        outputStream: OutputStream,
    ) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")

        if (indent > 0) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", indent.toString())
            transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", indent.toString())
        }

        val source = DOMSource(document)
        val result = StreamResult(outputStream)
        transformer.transform(source, result)
    }

    fun createDocument(graphic: ScalableVectorGraphic): Document {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("svg")
        val elementName = graphic.id
        if (elementName != null) {
            root.setAttribute("id", elementName)
        }
        val writtenRootForeign = graphic.foreign.withoutImpliedPresentationAttrs(InheritedStyle())
        for (item in writtenRootForeign) {
            root.setAttribute(item.key, item.value)
        }
        document.appendChild(root)

        // Generated def ids must not collide with ids of passthrough elements,
        // which may still be url-referenced from foreign attributes.
        val usedIds = collectExtraElementIds(graphic)

        // Assign stable ids to every ClipPath reachable via Group.clipPaths and every
        // gradient brush, and emit them under <defs> so refs below resolve.
        val clipPathIds = assignClipPathIds(graphic, usedIds)
        val gradientIds = assignGradientIds(graphic, usedIds)
        if (clipPathIds.isNotEmpty() || gradientIds.isNotEmpty()) {
            val defs = document.createElement("defs")
            root.appendChild(defs)
            for ((clipPath, id) in clipPathIds) {
                val clipPathElement = document.createElement("clipPath")
                clipPathElement.setAttribute("id", id)
                for ((key, value) in clipPath.foreign) {
                    clipPathElement.setAttribute(key, value)
                }
                val regionInherited = InheritedStyle()
                for (region in clipPath.regions) {
                    document.createChildElement(commandPrinter, clipPathElement, region, regionInherited, emptyMap(), gradientIds)
                }
                defs.appendChild(clipPathElement)
            }
            for ((gradient, id) in gradientIds) {
                defs.appendChild(document.createGradientDefElement(gradient, id))
            }
        }

        val inherited = graphic.foreign.toChildInheritedStyle(InheritedStyle())
        for (element in graphic.elements) {
            document.createChildElement(commandPrinter, root, element, inherited, clipPathIds, gradientIds)
        }

        return document
    }

    private fun collectExtraElementIds(graphic: ScalableVectorGraphic): MutableSet<String> {
        val ids = mutableSetOf<String>()

        fun walk(element: Element) {
            if (element is Extra) {
                element.id?.let(ids::add)
            }
            if (element is ContainerElement) {
                element.elements.forEach(::walk)
            }
        }
        graphic.elements.forEach(::walk)
        return ids
    }

    private fun assignClipPathIds(
        graphic: ScalableVectorGraphic,
        usedIds: MutableSet<String>,
    ): LinkedHashMap<ClipPath, String> {
        val result = LinkedHashMap<ClipPath, String>()
        var counter = 0

        fun nextId(preferred: String?): String {
            if (preferred != null && usedIds.add(preferred)) return preferred
            while (true) {
                val candidate = "clipPath${counter++}"
                if (usedIds.add(candidate)) return candidate
            }
        }

        fun walk(element: Element) {
            if (element is Group) {
                for (clipPath in element.clipPaths) {
                    if (clipPath !in result) {
                        result[clipPath] = nextId(clipPath.id)
                    }
                }
            }
            if (element is ContainerElement) {
                element.elements.forEach(::walk)
            }
        }
        graphic.elements.forEach(::walk)
        return result
    }

    private fun assignGradientIds(
        graphic: ScalableVectorGraphic,
        usedIds: MutableSet<String>,
    ): LinkedHashMap<Gradient, String> {
        val result = LinkedHashMap<Gradient, String>()
        var counter = 0

        fun nextId(): String {
            while (true) {
                val candidate = "gradient${counter++}"
                if (usedIds.add(candidate)) return candidate
            }
        }

        fun assign(brush: Brush) {
            if (brush is Gradient && brush !in result) {
                check(brush !is SweepGradient) { "SweepGradient cannot be represented in SVG" }
                result[brush] = nextId()
            }
        }

        fun walk(element: Element) {
            if (element is PaintedElement) {
                assign(element.effectiveFill)
                assign(element.effectiveStroke)
            }
            if (element is Group) {
                for (clipPath in element.clipPaths) {
                    clipPath.regions.forEach(::walk)
                }
            }
            if (element is ContainerElement) {
                element.elements.forEach(::walk)
            }
        }
        graphic.elements.forEach(::walk)
        return result
    }

    private fun Document.createGradientDefElement(
        gradient: Gradient,
        id: String,
    ): org.w3c.dom.Element {
        val formatter = commandPrinter.formatter

        val element =
            when (gradient) {
                is LinearGradient -> {
                    createElement("linearGradient").apply {
                        setAttribute("id", id)
                        setAttribute("gradientUnits", "userSpaceOnUse")
                        setAttribute("x1", formatter.format(gradient.startX))
                        setAttribute("y1", formatter.format(gradient.startY))
                        setAttribute("x2", formatter.format(gradient.endX))
                        setAttribute("y2", formatter.format(gradient.endY))
                    }
                }

                is RadialGradient -> {
                    createElement("radialGradient").apply {
                        setAttribute("id", id)
                        setAttribute("gradientUnits", "userSpaceOnUse")
                        setAttribute("cx", formatter.format(gradient.centerX))
                        setAttribute("cy", formatter.format(gradient.centerY))
                        setAttribute("r", formatter.format(gradient.radius))
                    }
                }

                is SweepGradient -> {
                    error("SweepGradient cannot be represented in SVG")
                }
            }

        val spreadMethod =
            when (gradient.tileMode()) {
                TileMode.CLAMP -> null
                TileMode.MIRROR -> "reflect"
                TileMode.REPEAT -> "repeat"
            }
        if (spreadMethod != null) {
            element.setAttribute("spreadMethod", spreadMethod)
        }

        for (stop in gradient.stops) {
            val stopElement = createElement("stop")
            stopElement.setAttribute("offset", formatter.format(stop.offset))

            // Alpha is expressed via stop-opacity for compatibility — SVG 1.1
            // renderers don't accept RGBA hex in stop-color.
            val opaque = Color(0xFF.toUByte(), stop.color.red, stop.color.green, stop.color.blue)
            stopElement.setAttribute("stop-color", Colors.NAMES_BY_COLORS[opaque] ?: opaque.toHexString(HexFormat.RGBA))
            if (stop.color.alpha != 0xFF.toUByte()) {
                stopElement.setAttribute("stop-opacity", formatter.format(stop.color.alpha.toFloat() / 0xFF))
            }

            element.appendChild(stopElement)
        }

        return element
    }

    private fun Gradient.tileMode(): TileMode =
        when (this) {
            is LinearGradient -> tileMode
            is RadialGradient -> tileMode
            is SweepGradient -> TileMode.CLAMP
        }

    private data class InheritedStyle(
        val fill: Brush = Colors.BLACK,
        val fillRule: Path.FillRule = Path.FillRule.NON_ZERO,
        val stroke: Brush = Colors.TRANSPARENT,
        val strokeWidth: Float = 1f,
        val strokeLineCap: Path.LineCap = Path.LineCap.BUTT,
        val strokeLineJoin: Path.LineJoin = Path.LineJoin.MITER,
        val strokeMiterLimit: Float = 4f,
    )

    private fun Map<String, String>.withoutImpliedPresentationAttrs(inherited: InheritedStyle): Map<String, String> {
        val inheritedFillColor = inherited.fill as? Color ?: Colors.BLACK
        val inheritedStrokeColor = inherited.stroke as? Color ?: Colors.TRANSPARENT
        return filter { (key, value) ->
            when (key) {
                // Unresolved paint server references parse as the default color and
                // would otherwise be mistaken for an implied attribute
                "fill" -> value.isUrlPaint() || (extractColor("fill", inheritedFillColor) ?: inherited.fill) != inherited.fill

                "fill-rule" -> (extractFillRule("fill-rule") ?: inherited.fillRule) != inherited.fillRule

                "stroke" -> value.isUrlPaint() || (extractColor("stroke", inheritedStrokeColor) ?: inherited.stroke) != inherited.stroke

                "stroke-width" -> this["stroke-width"]?.toFloatOrNull() != inherited.strokeWidth

                "stroke-linecap" -> (extractLineCap("stroke-linecap") ?: inherited.strokeLineCap) != inherited.strokeLineCap

                "stroke-linejoin" -> (extractLineJoin("stroke-linejoin") ?: inherited.strokeLineJoin) != inherited.strokeLineJoin

                "stroke-miterlimit" -> this["stroke-miterlimit"]?.toFloatOrNull() != inherited.strokeMiterLimit

                else -> true
            }
        }
    }

    // Shapes surface gradient paints through their brush properties; the
    // Color-typed fill/stroke are deprecated placeholders.
    private val PaintedElement.effectiveFill: Brush
        get() = if (this is Shape) fillBrush else fill

    private val PaintedElement.effectiveStroke: Brush
        get() = if (this is Shape) strokeBrush else stroke

    private fun org.w3c.dom.Element.writePaintAttributes(
        element: PaintedElement,
        inherited: InheritedStyle,
        gradientIds: Map<Gradient, String>,
    ) {
        when (val fill = element.effectiveFill) {
            inherited.fill -> {}

            is Color -> {
                if (fill.alpha == 0.toUByte()) {
                    setAttribute("fill", "none")
                } else {
                    val color = Colors.NAMES_BY_COLORS[fill] ?: fill.toHexString(HexFormat.RGBA)
                    setAttribute("fill", color)
                }
            }

            is Gradient -> {
                setAttribute("fill", "url(#${gradientIds.getValue(fill)})")
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

        when (val stroke = element.effectiveStroke) {
            inherited.stroke -> {}

            is Color -> {
                if (stroke.alpha == 0.toUByte()) {
                    setAttribute("stroke", "none")
                } else {
                    val color = Colors.NAMES_BY_COLORS[stroke] ?: stroke.toHexString(HexFormat.RGBA)
                    setAttribute("stroke", color)
                }
            }

            is Gradient -> {
                setAttribute("stroke", "url(#${gradientIds.getValue(stroke)})")
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

    private fun Map<String, String>.toChildInheritedStyle(current: InheritedStyle): InheritedStyle {
        val styleAttrs = this["style"]?.parseStyleAttribute() ?: emptyMap()
        val merged = this + styleAttrs
        val currentFillColor = current.fill as? Color ?: Colors.BLACK
        val currentStrokeColor = current.stroke as? Color ?: Colors.TRANSPARENT
        return InheritedStyle(
            fill = merged.extractColor("fill", currentFillColor) ?: current.fill,
            fillRule = merged.extractFillRule("fill-rule") ?: current.fillRule,
            stroke = merged.extractColor("stroke", currentStrokeColor) ?: current.stroke,
            strokeWidth = merged["stroke-width"]?.toFloatOrNull() ?: current.strokeWidth,
            strokeLineCap = merged.extractLineCap("stroke-linecap") ?: current.strokeLineCap,
            strokeLineJoin = merged.extractLineJoin("stroke-linejoin") ?: current.strokeLineJoin,
            strokeMiterLimit = merged["stroke-miterlimit"]?.toFloatOrNull() ?: current.strokeMiterLimit,
        )
    }

    private fun Document.createChildElement(
        commandPrinter: ScalableVectorGraphicCommandPrinter,
        parent: org.w3c.dom.Element,
        element: Element,
        inherited: InheritedStyle = InheritedStyle(),
        clipPathIds: Map<ClipPath, String> = emptyMap(),
        gradientIds: Map<Gradient, String> = emptyMap(),
    ) {
        val writtenForeign = element.foreign.withoutImpliedPresentationAttrs(inherited)
        val node =
            when (element) {
                is Path -> {
                    createElement("path").apply {
                        val data = element.commands.joinToString(separator = "", transform = commandPrinter::print)
                        setAttribute("d", data)
                        writePaintAttributes(element, inherited, gradientIds)
                    }
                }

                is Group -> {
                    val groupElement =
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
                                createChildElement(commandPrinter, node, child, childInherited, clipPathIds, gradientIds)
                            }
                        }

                    if (element.clipPaths.isEmpty()) {
                        groupElement
                    } else {
                        // SVG clip-path is single-valued; multiple clip paths intersect via
                        // synthesized wrapping <g> elements. Decorate the inner group with
                        // id+foreign here and skip the generic post-block.
                        val elementName = element.id
                        if (elementName != null) groupElement.setAttribute("id", elementName)
                        for ((k, v) in writtenForeign) groupElement.setAttribute(k, v)

                        var inner: org.w3c.dom.Element = groupElement
                        val firstId = clipPathIds[element.clipPaths.first()]
                        if (firstId != null) inner.setAttribute("clip-path", "url(#$firstId)")
                        for (extra in element.clipPaths.drop(1)) {
                            val id = clipPathIds[extra] ?: continue
                            val wrapper = createElement("g")
                            wrapper.setAttribute("clip-path", "url(#$id)")
                            wrapper.appendChild(inner)
                            inner = wrapper
                        }
                        parent.appendChild(inner)
                        null
                    }
                }

                is Extra -> {
                    createElement(element.name).also {
                        val childInherited = writtenForeign.toChildInheritedStyle(inherited)
                        for (child in element.elements) {
                            createChildElement(commandPrinter, it, child, childInherited, clipPathIds, gradientIds)
                        }
                    }
                }

                is Shape -> {
                    val fmt = commandPrinter.formatter
                    when (element) {
                        is Circle -> {
                            createElement("circle").apply {
                                setAttribute("cx", fmt.format(element.cx))
                                setAttribute("cy", fmt.format(element.cy))
                                setAttribute("r", fmt.format(element.r))
                            }
                        }

                        is Ellipse -> {
                            createElement("ellipse").apply {
                                setAttribute("cx", fmt.format(element.cx))
                                setAttribute("cy", fmt.format(element.cy))
                                setAttribute("rx", fmt.format(element.rx))
                                setAttribute("ry", fmt.format(element.ry))
                            }
                        }

                        is Rect -> {
                            createElement("rect").apply {
                                setAttribute("x", fmt.format(element.x))
                                setAttribute("y", fmt.format(element.y))
                                setAttribute("width", fmt.format(element.width))
                                setAttribute("height", fmt.format(element.height))
                                if (element.rx > 0f) setAttribute("rx", fmt.format(element.rx))
                                if (element.ry > 0f) setAttribute("ry", fmt.format(element.ry))
                            }
                        }

                        is Line -> {
                            createElement("line").apply {
                                setAttribute("x1", fmt.format(element.x1))
                                setAttribute("y1", fmt.format(element.y1))
                                setAttribute("x2", fmt.format(element.x2))
                                setAttribute("y2", fmt.format(element.y2))
                            }
                        }

                        is Polyline -> {
                            createElement("polyline").apply {
                                setAttribute("points", element.points.joinToString(" ") { "${fmt.format(it.x)},${fmt.format(it.y)}" })
                            }
                        }

                        is Polygon -> {
                            createElement("polygon").apply {
                                setAttribute("points", element.points.joinToString(" ") { "${fmt.format(it.x)},${fmt.format(it.y)}" })
                            }
                        }
                    }.also { it.writePaintAttributes(element, inherited, gradientIds) }
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
}
