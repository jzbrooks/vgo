package com.jzbrooks.vgo.svg

import com.jzbrooks.vgo.core.Brush
import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.GradientStop
import com.jzbrooks.vgo.core.LinearGradient
import com.jzbrooks.vgo.core.RadialGradient
import com.jzbrooks.vgo.core.TileMode
import com.jzbrooks.vgo.core.util.math.Matrix3
import com.jzbrooks.vgo.core.util.math.Rectangle
import com.jzbrooks.vgo.core.util.math.transformedOrNull
import com.jzbrooks.vgo.util.xml.asSequence
import org.w3c.dom.Node
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

val GRADIENT_ELEMENT_NAMES: Set<String> = hashSetOf("linearGradient", "radialGradient")
private const val MAX_HREF_DEPTH = 8

/**
 * Collects `<linearGradient>`/`<radialGradient>` definitions document-wide,
 * along with a count of every `url(#id)` paint reference to them, so that
 * fully-resolved definitions can be pruned from the parsed graphic.
 */
internal fun harvestGradientDefs(root: Node): SvgGradientDefs {
    val defsById = mutableMapOf<String, org.w3c.dom.Element>()
    val refCounts = mutableMapOf<String, Int>()

    fun countReferences(value: String) {
        for (match in urlReferenceRegex.findAll(value)) {
            val id = match.groupValues[1]
            refCounts.merge(id, 1, Int::plus)
        }
    }

    fun walk(node: Node) {
        if (node !is org.w3c.dom.Element) return

        if (node.nodeName in GRADIENT_ELEMENT_NAMES) {
            val id = node.getAttribute("id")
            if (id.isNotEmpty()) defsById[id] = node
        }

        for (attribute in arrayOf("fill", "stroke")) {
            if (node.hasAttribute(attribute)) countReferences(node.getAttribute(attribute))
        }
        if (node.hasAttribute("style")) {
            val style = node.getAttribute("style").parseStyleAttribute()
            style["fill"]?.let(::countReferences)
            style["stroke"]?.let(::countReferences)
        }

        node.childNodes.asSequence().forEach(::walk)
    }

    walk(root)

    return SvgGradientDefs(defsById, refCounts)
}

internal class SvgGradientDefs(
    private val defsById: Map<String, org.w3c.dom.Element>,
    private val refCounts: Map<String, Int>,
) {
    private val resolvedCounts = mutableMapOf<String, Int>()

    /**
     * Converts a `url(#id)` paint value into a [Brush] in the coordinate space of
     * the referencing element, or returns null when the reference isn't exactly
     * representable (the caller falls back to verbatim passthrough).
     *
     * [bounds] supplies the referencing element's bounding box and is only
     * invoked for gradients with objectBoundingBox units.
     */
    fun resolveBrush(
        rawValue: String,
        bounds: () -> Rectangle?,
    ): Brush? {
        val id = rawValue.extractUrlReferenceOrNull() ?: return null
        val brush = buildBrush(id, bounds) ?: return null
        resolvedCounts.merge(id, 1, Int::plus)
        return brush
    }

    fun requiresObjectBounds(rawValue: String): Boolean {
        val id = rawValue.extractUrlReferenceOrNull() ?: return false
        val chain = templateChain(id) ?: return false

        fun effectiveAttribute(name: String): String? = chain.firstNotNullOfOrNull { it.getAttribute(name).ifEmpty { null } }

        return (effectiveAttribute("gradientUnits") ?: "objectBoundingBox") == "objectBoundingBox"
    }

    /** Ids who's every document-wide reference resolved to a typed brush. */
    fun fullyConsumedIds(): Set<String> = resolvedCounts.filterKeys { resolvedCounts[it] == refCounts[it] }.keys

    private fun buildBrush(
        id: String,
        bounds: () -> Rectangle?,
    ): Brush? {
        val chain = templateChain(id) ?: return null
        val definition = chain.first()

        fun effectiveAttribute(name: String): String? = chain.firstNotNullOfOrNull { it.getAttribute(name).ifEmpty { null } }

        val stops = parseStops(chain) ?: return null
        when (stops.size) {
            // Per the spec, a gradient without stops renders as if fill="none"
            0 -> return Colors.TRANSPARENT

            1 -> return stops.single().color
        }

        val objectBoundingBox =
            when (effectiveAttribute("gradientUnits") ?: "objectBoundingBox") {
                "objectBoundingBox" -> true
                "userSpaceOnUse" -> false
                else -> return null
            }

        val tileMode =
            when (effectiveAttribute("spreadMethod") ?: "pad") {
                "pad" -> TileMode.CLAMP
                "reflect" -> TileMode.MIRROR
                "repeat" -> TileMode.REPEAT
                else -> return null
            }

        fun coordinate(
            name: String,
            defaultPercent: Float,
        ): Float? = parseCoordinate(effectiveAttribute(name), defaultPercent, objectBoundingBox)

        val gradient =
            when (definition.nodeName) {
                "linearGradient" -> {
                    LinearGradient(
                        startX = coordinate("x1", 0f) ?: return null,
                        startY = coordinate("y1", 0f) ?: return null,
                        endX = coordinate("x2", 100f) ?: return null,
                        endY = coordinate("y2", 0f) ?: return null,
                        stops = stops,
                        tileMode = tileMode,
                    )
                }

                "radialGradient" -> {
                    val centerX = coordinate("cx", 50f) ?: return null
                    val centerY = coordinate("cy", 50f) ?: return null

                    // The gradient model has no focal point
                    if (effectiveAttribute("fx") != null && coordinate("fx", 50f)?.takeIf { abs(it - centerX) < 1e-4 } == null) return null
                    if (effectiveAttribute("fy") != null && coordinate("fy", 50f)?.takeIf { abs(it - centerY) < 1e-4 } == null) return null

                    RadialGradient(
                        centerX = centerX,
                        centerY = centerY,
                        radius = coordinate("r", 50f) ?: return null,
                        stops = stops,
                        tileMode = tileMode,
                    )
                }

                else -> {
                    return null
                }
            }

        val gradientTransform =
            effectiveAttribute("gradientTransform")?.let { parseTransformList(it) ?: return null }
                ?: Matrix3.IDENTITY

        val unitsTransform =
            if (objectBoundingBox) {
                val box = bounds() ?: return null
                val width = box.right - box.left
                val height = box.top - box.bottom
                Matrix3.from(floatArrayOf(width, 0f, box.left, 0f, height, box.bottom, 0f, 0f, 1f))
            } else {
                Matrix3.IDENTITY
            }

        return gradient.transformedOrNull(unitsTransform * gradientTransform)
    }

    /**
     * The definition followed by its href/xlink:href template ancestors.
     * Null when a reference is missing, cyclic, or unreasonably deep.
     */
    private fun templateChain(id: String): List<org.w3c.dom.Element>? {
        val chain = mutableListOf<org.w3c.dom.Element>()
        val visited = mutableSetOf<String>()
        var currentId: String? = id

        while (currentId != null) {
            if (!visited.add(currentId) || visited.size > MAX_HREF_DEPTH) return null
            val definition = defsById[currentId] ?: return null
            chain.add(definition)

            val href =
                definition.getAttribute("href").ifEmpty { null }
                    ?: definition.getAttribute("xlink:href").ifEmpty { null }
            currentId = href?.let { if (it.startsWith("#")) it.substring(1) else return null }
        }

        return chain
    }

    /** Stops come from the first definition in the chain that declares any. */
    private fun parseStops(chain: List<org.w3c.dom.Element>): List<GradientStop>? {
        val stopElements =
            chain.firstNotNullOfOrNull { definition ->
                definition.childNodes
                    .asSequence()
                    .filterIsInstance<org.w3c.dom.Element>()
                    .filter { it.nodeName == "stop" }
                    .toList()
                    .ifEmpty { null }
            } ?: emptyList()

        val stops = mutableListOf<GradientStop>()
        var previousOffset = 0f
        for (element in stopElements) {
            val attributes =
                element.attributes
                    .asSequence()
                    .associate { it.nodeName to it.nodeValue }
            val style = attributes["style"]?.parseStyleAttribute() ?: emptyMap()
            val merged = attributes + style

            val offset = (parseFraction(merged["offset"]) ?: 0f).coerceIn(previousOffset, 1f)
            previousOffset = offset

            val color = parseStopColor(merged["stop-color"] ?: "black") ?: return null
            val opacity = (parseFraction(merged["stop-opacity"]) ?: 1f).coerceIn(0f, 1f)
            val alpha = (color.alpha.toInt() * opacity).roundToInt().toUByte()

            stops.add(GradientStop(offset, Color(alpha, color.red, color.green, color.blue)))
        }

        return stops
    }

    private fun parseStopColor(value: String): Color? {
        val trimmed = value.trim()
        val parseable =
            trimmed.startsWith("#") ||
                trimmed.startsWith("rgb") ||
                Colors.COLORS_BY_NAMES.containsKey(trimmed)
        if (!parseable) return null
        return parseColorValue(trimmed, Colors.BLACK)
    }

    /** A plain float or a percentage, normalized to a fraction. */
    private fun parseFraction(value: String?): Float? {
        if (value == null) return null
        val trimmed = value.trim()
        return if (trimmed.endsWith("%")) {
            trimmed.dropLast(1).toFloatOrNull()?.div(100f)
        } else {
            trimmed.toFloatOrNull()
        }
    }

    /**
     * Percentages are fractions of the bounding box in objectBoundingBox units,
     * but fractions of the viewport in user space — viewport resolution is out of
     * scope, so nonzero user-space percentages aren't representable.
     */
    private fun parseCoordinate(
        value: String?,
        defaultPercent: Float,
        objectBoundingBox: Boolean,
    ): Float? {
        val percent =
            if (value == null) {
                defaultPercent
            } else {
                val trimmed = value.trim()
                if (trimmed.endsWith("%")) {
                    trimmed.dropLast(1).toFloatOrNull() ?: return null
                } else {
                    return trimmed.toFloatOrNull()
                }
            }

        return when {
            objectBoundingBox -> percent / 100f
            percent == 0f -> 0f
            else -> null
        }
    }

    private fun parseTransformList(value: String): Matrix3? {
        val functionRegex = Regex("""(\w+)\s*\(([^)]*)\)""")
        var matrix: Matrix3 = Matrix3.IDENTITY
        var consumed = 0

        for (match in functionRegex.findAll(value)) {
            // Anything between functions other than whitespace/commas is malformed
            if (value.substring(consumed, match.range.first).any { !it.isWhitespace() && it != ',' }) return null
            consumed = match.range.last + 1

            val arguments =
                match.groupValues[2]
                    .split(',', ' ', '\t', '\n', '\r')
                    .filter(String::isNotEmpty)
                    .map { it.toFloatOrNull() ?: return null }

            val transform =
                when (match.groupValues[1]) {
                    "matrix" -> {
                        if (arguments.size != 6) return null
                        val (a, b, c, d) = arguments
                        Matrix3.from(floatArrayOf(a, c, arguments[4], b, d, arguments[5], 0f, 0f, 1f))
                    }

                    "translate" -> {
                        if (arguments.isEmpty() || arguments.size > 2) return null
                        Matrix3.from(floatArrayOf(1f, 0f, arguments[0], 0f, 1f, arguments.getOrElse(1) { 0f }, 0f, 0f, 1f))
                    }

                    "scale" -> {
                        if (arguments.isEmpty() || arguments.size > 2) return null
                        Matrix3.from(floatArrayOf(arguments[0], 0f, 0f, 0f, arguments.getOrElse(1) { arguments[0] }, 0f, 0f, 0f, 1f))
                    }

                    "rotate" -> {
                        if (arguments.size != 1 && arguments.size != 3) return null
                        val radians = arguments[0] * PI.toFloat() / 180f
                        val rotation =
                            Matrix3.from(floatArrayOf(cos(radians), -sin(radians), 0f, sin(radians), cos(radians), 0f, 0f, 0f, 1f))
                        if (arguments.size == 3) {
                            val pivot = Matrix3.from(floatArrayOf(1f, 0f, arguments[1], 0f, 1f, arguments[2], 0f, 0f, 1f))
                            val pivotInverse = Matrix3.from(floatArrayOf(1f, 0f, -arguments[1], 0f, 1f, -arguments[2], 0f, 0f, 1f))
                            pivot * rotation * pivotInverse
                        } else {
                            rotation
                        }
                    }

                    else -> {
                        return null
                    }
                }

            matrix *= transform
        }

        if (value.substring(consumed).any { !it.isWhitespace() && it != ',' }) return null

        return matrix
    }
}
