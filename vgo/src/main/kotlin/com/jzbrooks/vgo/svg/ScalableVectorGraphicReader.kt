package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Circle
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.ContainerElement
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Ellipse
import com.jzbrooks.vgo.core.graphic.Extra
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Line
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.Polygon
import com.jzbrooks.vgo.core.graphic.Polyline
import com.jzbrooks.vgo.core.graphic.Rect
import com.jzbrooks.vgo.core.graphic.Shape
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.transformation.ConvertShapesToPaths
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.Rectangle
import com.jzbrooks.vgo.core.util.math.Surveyor
import com.jzbrooks.vgo.util.xml.asSequence
import com.jzbrooks.vgo.util.xml.removeFloatOrNull
import com.jzbrooks.vgo.util.xml.removeOrNull
import com.jzbrooks.vgo.util.xml.toMutableMap
import org.w3c.dom.Comment
import org.w3c.dom.NamedNodeMap
import org.w3c.dom.Node
import org.w3c.dom.Text

fun parse(root: Node): ScalableVectorGraphic {
    val rootStyleProperties =
        root.attributes
            .getNamedItem("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()
    val rootAttrs = root.attributes.asSequence().associate { it.nodeName to it.nodeValue }

    // Style properties should overwrite inherited attributes
    val inherited = rootAttrs + rootStyleProperties

    // Pre-pass: harvest gradient defs anywhere in the document so that
    // fill/stroke url(#id) references resolve to typed brushes during the
    // main parse. Unresolvable defs remain Extra passthrough elements.
    val gradients = harvestGradientDefs(root)

    // Pre-pass: harvest <clipPath id="..."> defs anywhere in the document (typically
    // inside <defs>, but the spec allows them anywhere). They're skipped by the main
    // parse below — only resolved references via Group.clipPaths surface them in IR.
    val clipPathDefs = harvestClipPathDefs(root, gradients, inherited)

    val elements =
        root.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, gradients, inherited) }
            .toList()

    resolveClipPathReferences(elements, clipPathDefs)

    return ScalableVectorGraphic(
        pruneConsumedGradientDefs(elements, gradients.fullyConsumedIds()),
        root.attributes.removeOrNull("id")?.nodeValue,
        root.attributes.toMutableMap(),
    )
}

/**
 * Removes gradient definition elements whose every reference was converted to a
 * typed brush, along with any <defs> containers that end up empty. Definitions
 * with unresolved (passthrough) references must survive so those references
 * keep rendering.
 */
private fun pruneConsumedGradientDefs(
    elements: List<Element>,
    consumedIds: Set<String>,
): List<Element> {
    fun prune(elements: List<Element>): List<Element> =
        elements.mapNotNull { element ->
            when (element) {
                is Extra if element.name in SvgGradientDefs.GRADIENT_ELEMENT_NAMES && element.id in consumedIds -> {
                    null
                }

                is ContainerElement -> {
                    element.elements = prune(element.elements)
                    val prunableDefs =
                        element is Extra &&
                            element.name == "defs" &&
                            element.elements.isEmpty() &&
                            element.id == null &&
                            element.foreign.isEmpty()
                    if (prunableDefs) null else element
                }

                else -> {
                    element
                }
            }
        }

    return prune(elements)
}

private fun harvestClipPathDefs(
    root: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String>,
): Map<String, ClipPath> {
    val defs = mutableMapOf<String, ClipPath>()

    fun walk(
        node: Node,
        ctx: Map<String, String>,
    ) {
        if (node is Text || node is Comment) return
        if (node.nodeName == "clipPath") {
            val def = parseClipPathDef(node, gradients, ctx)
            val defId = def.id
            if (defId != null) defs[defId] = def
            return
        }
        val childCtx = collectStyleInheritance(node, ctx)
        node.childNodes.asSequence().forEach { walk(it, childCtx) }
    }

    root.childNodes.asSequence().forEach { walk(it, inherited) }
    return defs
}

private fun parseClipPathDef(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String>,
): ClipPath {
    val childInherited = collectStyleInheritance(node, inherited)

    val regions =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, gradients, childInherited) }
            .mapNotNull { child ->
                when (child) {
                    is Path -> child
                    is Shape -> ConvertShapesToPaths.convertToPath(child)
                    else -> null
                }
            }.toList()

    node.attributes.removeOrNull("style")

    return ClipPath(
        regions,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun resolveClipPathReferences(
    elements: List<Element>,
    defs: Map<String, ClipPath>,
) {
    fun walk(element: Element) {
        if (element is Group) {
            val ref = element.foreign["clip-path"]
            val id = ref?.let { urlReferenceRegex.find(it)?.groupValues?.get(1) }
            val def = id?.let(defs::get)
            if (def != null) {
                element.clipPaths = listOf(def)
                element.foreign.remove("clip-path")
            }
        }
        if (element is ContainerElement) {
            element.elements.forEach(::walk)
        }
    }
    elements.forEach(::walk)
}

private fun parseElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Element? {
    if (node is Text || node is Comment) return null

    return when (node.nodeName) {
        "g" -> parseGroupElement(node, gradients, inherited)
        "clipPath" -> null
        "path" -> parsePathElement(node, gradients, inherited)
        "circle" -> parseCircleElement(node, gradients, inherited)
        "ellipse" -> parseEllipseElement(node, gradients, inherited)
        "rect" -> parseRectElement(node, gradients, inherited)
        "line" -> parseLineElement(node, gradients, inherited)
        "polyline" -> parsePolylineElement(node, gradients, inherited)
        "polygon" -> parsePolygonElement(node, gradients, inherited)
        else -> parseExtraElement(node, gradients, inherited)
    }
}

private fun parseGroupElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Group {
    val childInherited = collectStyleInheritance(node, inherited)

    val childElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, gradients, childInherited) }
            .toList()

    node.attributes.removeOrNull("style")

    // This has to happen before foreign property collection
    val transform = node.attributes.extractTransformMatrix()
    return Group(
        childElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
        transform,
    )
}

/**
 * Builds the inherited presentation attribute context for children of [node].
 * Precedence: ancestor inherited < node's own presentation attributes < node's style attribute
 */
private fun collectStyleInheritance(
    node: Node,
    inherited: Map<String, String>,
): Map<String, String> {
    val styleProperties =
        node.attributes
            .getNamedItem("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()
    val nodeAttrs = node.attributes.asSequence().associate { it.nodeName to it.nodeValue }

    // The order of concatenation is important for precedence.
    return inherited + nodeAttrs + styleProperties
}

/**
 * Merges inherited, node-level, and style presentation attributes, removes them
 * from the DOM node (so they don't leak into [Path.foreign]), and returns the resolved values.
 *
 * Gradient references (`url(#id)`) on the element itself are preserved as raw
 * values so they can be resolved after the element is built and its bounds are
 * available uniformly.
 */
private fun extractMergedPresentationAttributes(
    node: Node,
    inherited: Map<String, String>,
): PresentationAttributes {
    val styleProperties =
        node.attributes
            .removeOrNull("style")
            ?.nodeValue
            ?.parseStyleAttribute() ?: emptyMap()

    // Snapshot presentation attrs before removing them so they don't appear in foreign
    val nodeAttrMap =
        node.attributes
            .asSequence()
            .filter { it.nodeName in PRESENTATION_ATTRIBUTES }
            .associate { it.nodeName to it.nodeValue }

    for (attr in PRESENTATION_ATTRIBUTES) {
        node.attributes.removeOrNull(attr)
    }

    // Style properties outside PRESENTATION_ATTRIBUTES (e.g. opacity, display,
    // stroke-dasharray) aren't lifted into typed fields; re-attach them so the
    // shape parser captures them in foreign and the writer emits them verbatim.
    val unextractedStyle = styleProperties.filterKeys { it !in PRESENTATION_ATTRIBUTES }.toMutableMap()

    // The order of concatenation is important for precedence.
    // style > node attrs > inherited > CSS initial value
    val merged = inherited + nodeAttrMap + styleProperties
    val ownPaints = mutableMapOf<String, OwnPaint>()

    fun resolvePaint(
        key: String,
        default: Color,
    ): Brush {
        // Only references on the element itself resolve — a reference inherited
        // from an ancestor stays in the ancestor's foreign attributes and SVG
        // paint inheritance keeps the output correct.
        val ownStyle = styleProperties[key]
        val ownAttribute = nodeAttrMap[key]
        val own = ownStyle ?: ownAttribute
        if (own != null && own.isUrlPaint()) {
            ownPaints[key] = OwnPaint(own, if (ownStyle != null) PaintSource.STYLE else PaintSource.ATTRIBUTE)
            return default
        }
        return merged.extractColor(key, default) ?: default
    }

    val presentation =
        PresentationAttributes(
            fill = resolvePaint("fill", Colors.BLACK),
            fillRule = merged.extractFillRule("fill-rule") ?: Path.FillRule.NON_ZERO,
            stroke = resolvePaint("stroke", Colors.TRANSPARENT),
            strokeWidth = merged["stroke-width"]?.toFloatOrNull() ?: 1f,
            strokeLineCap = merged.extractLineCap("stroke-linecap") ?: Path.LineCap.BUTT,
            strokeLineJoin = merged.extractLineJoin("stroke-linejoin") ?: Path.LineJoin.MITER,
            strokeMiterLimit = merged["stroke-miterlimit"]?.toFloatOrNull() ?: 4f,
            ownPaints = OwnPaints(ownPaints["fill"], ownPaints["stroke"]),
        )

    if (unextractedStyle.isNotEmpty()) {
        val serialized = unextractedStyle.entries.joinToString(";") { (k, v) -> "$k:$v" }
        val attr = node.ownerDocument.createAttribute("style")
        attr.value = serialized
        node.attributes.setNamedItem(attr)
    }

    return presentation
}

// The typed Color properties on shapes cannot represent gradients; they hold
// the CSS initial values as placeholders while the brush carries the true paint.
private val PresentationAttributes.fillColor: Color
    get() = fill as? Color ?: Colors.BLACK

private val PresentationAttributes.strokeColor: Color
    get() = stroke as? Color ?: Colors.TRANSPARENT

private fun <T : Shape> T.applyBrushes(presentation: PresentationAttributes): T =
    apply {
        fillBrush = presentation.fill
        strokeBrush = presentation.stroke
    }

private fun <T : Element> T.resolveElementGradientPaints(
    ownPaints: OwnPaints,
    gradients: SvgGradientDefs,
): T {
    var element: Element = this

    fun resolve(rawPaint: OwnPaint): Brush? {
        val bounds =
            if (gradients.requiresObjectBounds(rawPaint.rawValue)) {
                { element.paintBoundsOrNull() }
            } else {
                { null }
            }
        return gradients.resolveBrush(rawPaint.rawValue, bounds)
    }

    ownPaints.fill?.let { rawPaint ->
        val brush = resolve(rawPaint)
        if (brush == null) {
            element.restoreRawPaint("fill", rawPaint)
        } else {
            element =
                when (val current = element) {
                    is Path -> current.copy(fill = brush)
                    is Shape -> current.apply { fillBrush = brush }
                    else -> current
                }
        }
    }

    ownPaints.stroke?.let { rawPaint ->
        val brush = resolve(rawPaint)
        if (brush == null) {
            element.restoreRawPaint("stroke", rawPaint)
        } else {
            element =
                when (val current = element) {
                    is Path -> current.copy(stroke = brush)
                    is Shape -> current.apply { strokeBrush = brush }
                    else -> current
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    return element as T
}

private fun Element.restoreRawPaint(
    key: String,
    rawPaint: OwnPaint,
) {
    when (rawPaint.source) {
        PaintSource.ATTRIBUTE -> {
            foreign[key] = rawPaint.rawValue
        }

        PaintSource.STYLE -> {
            val style = foreign["style"]?.parseStyleAttribute()?.toMutableMap() ?: mutableMapOf()
            style[key] = rawPaint.rawValue
            foreign["style"] = style.entries.joinToString(";") { (k, v) -> "$k:$v" }
        }
    }
}

private fun Element.paintBoundsOrNull(): Rectangle? {
    val surveyor = Surveyor()

    return when (this) {
        is Path -> if (commands.isEmpty()) null else surveyor.findBoundingBox(commands)
        is Circle -> Rectangle(cx - r, cy + r, cx + r, cy - r)
        is Ellipse -> Rectangle(cx - rx, cy + ry, cx + rx, cy - ry)
        is Rect -> Rectangle(x, y + height, x + width, y)
        is Line -> Rectangle(minOf(x1, x2), maxOf(y1, y2), maxOf(x1, x2), minOf(y1, y2))
        is Polyline -> points.boundsOrNull()
        is Polygon -> points.boundsOrNull()
        else -> null
    }
}

private fun parsePathElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Path {
    val commands =
        CommandString(
            node.attributes
                .removeNamedItem("d")
                .nodeValue
                .toString(),
        ).toCommandList()
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Path(
        id,
        node.attributes.toMutableMap(),
        commands,
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parseCircleElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Circle {
    val cx = node.attributes.removeFloatOrNull("cx") ?: 0f
    val cy = node.attributes.removeFloatOrNull("cy") ?: 0f
    val r = node.attributes.removeFloatOrNull("r") ?: 0f
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Circle(
        id,
        node.attributes.toMutableMap(),
        cx,
        cy,
        r,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parseEllipseElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Ellipse {
    val cx = node.attributes.removeFloatOrNull("cx") ?: 0f
    val cy = node.attributes.removeFloatOrNull("cy") ?: 0f
    val rx = node.attributes.removeFloatOrNull("rx") ?: 0f
    val ry = node.attributes.removeFloatOrNull("ry") ?: 0f
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Ellipse(
        id,
        node.attributes.toMutableMap(),
        cx,
        cy,
        rx,
        ry,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parseRectElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Rect {
    val x = node.attributes.removeFloatOrNull("x") ?: 0f
    val y = node.attributes.removeFloatOrNull("y") ?: 0f
    val width = node.attributes.removeFloatOrNull("width") ?: 0f
    val height = node.attributes.removeFloatOrNull("height") ?: 0f
    val rawRx = node.attributes.removeFloatOrNull("rx")
    val rawRy = node.attributes.removeFloatOrNull("ry")
    val rx = (rawRx ?: rawRy ?: 0f).coerceAtMost(width / 2)
    val ry = (rawRy ?: rawRx ?: 0f).coerceAtMost(height / 2)
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Rect(
        id,
        node.attributes.toMutableMap(),
        x,
        y,
        width,
        height,
        rx,
        ry,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parseLineElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Line {
    val x1 = node.attributes.removeFloatOrNull("x1") ?: 0f
    val y1 = node.attributes.removeFloatOrNull("y1") ?: 0f
    val x2 = node.attributes.removeFloatOrNull("x2") ?: 0f
    val y2 = node.attributes.removeFloatOrNull("y2") ?: 0f
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Line(
        id,
        node.attributes.toMutableMap(),
        x1,
        y1,
        x2,
        y2,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parsePolylineElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Polyline {
    val points = parsePoints(node.attributes.removeOrNull("points")?.nodeValue ?: "")
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Polyline(
        id,
        node.attributes.toMutableMap(),
        points,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parsePolygonElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Polygon {
    val points = parsePoints(node.attributes.removeOrNull("points")?.nodeValue ?: "")
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Polygon(
        id,
        node.attributes.toMutableMap(),
        points,
        presentation.fillColor,
        presentation.fillRule,
        presentation.strokeColor,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    ).applyBrushes(presentation).resolveElementGradientPaints(presentation.ownPaints, gradients)
}

private fun parsePoints(value: String): List<Point> {
    val numberPattern = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")

    val numbers = numberPattern.findAll(value).map { it.value.toFloat() }.toList()
    return numbers.chunked(2).mapNotNull { pair ->
        if (pair.size == 2) Point(pair[0], pair[1]) else null
    }
}

private fun List<Point>.boundsOrNull(): Rectangle? {
    if (isEmpty()) return null
    return Rectangle(
        left = minOf { it.x },
        top = maxOf { it.y },
        right = maxOf { it.x },
        bottom = minOf { it.y },
    )
}

private fun parseExtraElement(
    node: Node,
    gradients: SvgGradientDefs,
    inherited: Map<String, String> = emptyMap(),
): Extra {
    val childInherited = collectStyleInheritance(node, inherited)

    val containedElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, gradients, childInherited) }
            .toList()

    return Extra(
        node.nodeValue ?: node.nodeName,
        containedElements,
        node.attributes.removeOrNull("id")?.nodeValue,
        node.attributes.toMutableMap(),
    )
}

private fun NamedNodeMap.extractTransformMatrix(): Matrix3 {
    val transform = removeOrNull("transform")?.nodeValue ?: return Matrix3.IDENTITY

    val entries =
        transform
            .removePrefix("matrix(")
            .trimEnd(')')
            .split(',')
            .map(String::toFloat)

    return Matrix3.from(
        floatArrayOf(entries[0], entries[2], entries[4], entries[1], entries[3], entries[5], 0f, 0f, 1f),
    )
}

private data class PresentationAttributes(
    val fill: Brush,
    val fillRule: Path.FillRule,
    val stroke: Brush,
    val strokeWidth: Float,
    val strokeLineCap: Path.LineCap,
    val strokeLineJoin: Path.LineJoin,
    val strokeMiterLimit: Float,
    val ownPaints: OwnPaints,
)

private data class OwnPaints(
    val fill: OwnPaint?,
    val stroke: OwnPaint?,
)

private data class OwnPaint(
    val rawValue: String,
    val source: PaintSource,
)

private enum class PaintSource {
    ATTRIBUTE,
    STYLE,
}
