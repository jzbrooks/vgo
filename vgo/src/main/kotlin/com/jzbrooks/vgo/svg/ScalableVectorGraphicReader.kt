package com.jzbrooks.vgo.svg

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

    // Pre-pass: harvest <clipPath id="..."> defs anywhere in the document (typically
    // inside <defs>, but the spec allows them anywhere). They're skipped by the main
    // parse below — only resolved references via Group.clipPaths surface them in IR.
    val clipPathDefs = harvestClipPathDefs(root, inherited)

    val elements =
        root.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, inherited) }
            .toList()

    resolveClipPathReferences(elements, clipPathDefs)

    return ScalableVectorGraphic(
        elements,
        root.attributes.removeOrNull("id")?.nodeValue,
        root.attributes.toMutableMap(),
    )
}

private fun harvestClipPathDefs(
    root: Node,
    inherited: Map<String, String>,
): Map<String, ClipPath> {
    val defs = mutableMapOf<String, ClipPath>()

    fun walk(
        node: Node,
        ctx: Map<String, String>,
    ) {
        if (node is Text || node is Comment) return
        if (node.nodeName == "clipPath") {
            val def = parseClipPathDef(node, ctx)
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
    inherited: Map<String, String>,
): ClipPath {
    val childInherited = collectStyleInheritance(node, inherited)

    val regions =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
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

private val clipPathUrlRegex = Regex("""url\(\s*["']?#([^)\s"']+)["']?\s*\)""")

private fun resolveClipPathReferences(
    elements: List<Element>,
    defs: Map<String, ClipPath>,
) {
    fun walk(element: Element) {
        if (element is Group) {
            val ref = element.foreign["clip-path"]
            val id = ref?.let { clipPathUrlRegex.find(it)?.groupValues?.get(1) }
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
    inherited: Map<String, String> = emptyMap(),
): Element? {
    if (node is Text || node is Comment) return null

    return when (node.nodeName) {
        "g" -> parseGroupElement(node, inherited)
        "clipPath" -> null
        "path" -> parsePathElement(node, inherited)
        "circle" -> parseCircleElement(node, inherited)
        "ellipse" -> parseEllipseElement(node, inherited)
        "rect" -> parseRectElement(node, inherited)
        "line" -> parseLineElement(node, inherited)
        "polyline" -> parsePolylineElement(node, inherited)
        "polygon" -> parsePolygonElement(node, inherited)
        else -> parseExtraElement(node, inherited)
    }
}

private fun parseGroupElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Group {
    val childInherited = collectStyleInheritance(node, inherited)

    val childElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
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
 * from the DOM node (so they don't leak into [foreign]), and returns the resolved values.
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
    val unextractedStyle = styleProperties.filterKeys { it !in PRESENTATION_ATTRIBUTES }
    if (unextractedStyle.isNotEmpty()) {
        val serialized = unextractedStyle.entries.joinToString(";") { (k, v) -> "$k:$v" }
        val attr = node.ownerDocument.createAttribute("style")
        attr.value = serialized
        node.attributes.setNamedItem(attr)
    }

    // The order of concatenation is important for precedence.
    // style > node attrs > inherited > CSS initial value
    val merged = inherited + nodeAttrMap + styleProperties

    return PresentationAttributes(
        fill = merged.extractColor("fill", Colors.BLACK) ?: Colors.BLACK,
        fillRule = merged.extractFillRule("fill-rule") ?: Path.FillRule.NON_ZERO,
        stroke = merged.extractColor("stroke", Colors.TRANSPARENT) ?: Colors.TRANSPARENT,
        strokeWidth = merged["stroke-width"]?.toFloatOrNull() ?: 1f,
        strokeLineCap = merged.extractLineCap("stroke-linecap") ?: Path.LineCap.BUTT,
        strokeLineJoin = merged.extractLineJoin("stroke-linejoin") ?: Path.LineJoin.MITER,
        strokeMiterLimit = merged["stroke-miterlimit"]?.toFloatOrNull() ?: 4f,
    )
}

private data class PresentationAttributes(
    val fill: com.jzbrooks.vgo.core.Color,
    val fillRule: Path.FillRule,
    val stroke: com.jzbrooks.vgo.core.Color,
    val strokeWidth: Float,
    val strokeLineCap: Path.LineCap,
    val strokeLineJoin: Path.LineJoin,
    val strokeMiterLimit: Float,
)

private fun parsePathElement(
    node: Node,
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
    )
}

private fun parseCircleElement(
    node: Node,
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
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private fun parseEllipseElement(
    node: Node,
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
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private fun parseRectElement(
    node: Node,
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
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private fun parseLineElement(
    node: Node,
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
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private fun parsePolylineElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Polyline {
    val points = parsePoints(node.attributes.removeOrNull("points")?.nodeValue ?: "")
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Polyline(
        id,
        node.attributes.toMutableMap(),
        points,
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private fun parsePolygonElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Polygon {
    val points = parsePoints(node.attributes.removeOrNull("points")?.nodeValue ?: "")
    val id = node.attributes.removeOrNull("id")?.nodeValue
    val presentation = extractMergedPresentationAttributes(node, inherited)

    return Polygon(
        id,
        node.attributes.toMutableMap(),
        points,
        presentation.fill,
        presentation.fillRule,
        presentation.stroke,
        presentation.strokeWidth,
        presentation.strokeLineCap,
        presentation.strokeLineJoin,
        presentation.strokeMiterLimit,
    )
}

private val numberPattern = Regex("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?")

private fun parsePoints(value: String): List<Point> {
    val numbers = numberPattern.findAll(value).map { it.value.toFloat() }.toList()
    return numbers.chunked(2).mapNotNull { pair ->
        if (pair.size == 2) Point(pair[0], pair[1]) else null
    }
}

private fun parseExtraElement(
    node: Node,
    inherited: Map<String, String> = emptyMap(),
): Extra {
    val childInherited = collectStyleInheritance(node, inherited)

    val containedElements =
        node.childNodes
            .asSequence()
            .mapNotNull { parseElement(it, childInherited) }
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
