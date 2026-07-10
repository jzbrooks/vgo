@file:OptIn(ExperimentalVgoApi::class)

package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.ClipPath
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.EllipticalArcCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.QuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothCubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.SmoothQuadraticBezierCurve
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.computeTransformation
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

internal const val FOREIGN_KEY_PROPERTY_NAME = "propertyName"
internal const val FOREIGN_KEY_PACKAGE_NAME = "packageName"

private val BUILDER_PARAMETERS =
    listOf(
        "name",
        "defaultWidth",
        "defaultHeight",
        "viewportWidth",
        "viewportHeight",
        "tintColor",
        "tintBlendMode",
        "autoMirror",
    )

private val PATH_PARAMETERS =
    listOf(
        "name",
        "fill",
        "fillAlpha",
        "stroke",
        "strokeAlpha",
        "strokeLineWidth",
        "strokeLineCap",
        "strokeLineJoin",
        "strokeLineMiter",
        "pathFillType",
        "pathBuilder",
    )

private val PATH_PARAMETER_ALIASES = mapOf("strokeWidth" to "strokeLineWidth")

private val GROUP_PARAMETERS =
    listOf(
        "name",
        "rotate",
        "pivotX",
        "pivotY",
        "scaleX",
        "scaleY",
        "translationX",
        "translationY",
        "clipPathData",
    )

private val CUBIC_PARAMETERS = listOf("x1", "y1", "x2", "y2", "x3", "y3")
private val RELATIVE_CUBIC_PARAMETERS = listOf("dx1", "dy1", "dx2", "dy2", "dx3", "dy3")
private val TWO_POINT_PARAMETERS = listOf("x1", "y1", "x2", "y2")
private val RELATIVE_TWO_POINT_PARAMETERS = listOf("dx1", "dy1", "dx2", "dy2")
private val ARC_PARAMETERS =
    listOf("horizontalEllipseRadius", "verticalEllipseRadius", "theta", "isMoreThanHalf", "isPositiveArc", "x1", "y1")
private val RELATIVE_ARC_PARAMETERS = listOf("a", "b", "theta", "isMoreThanHalf", "isPositiveArc", "dx1", "dy1")

// PathNode constructors share names with the PathBuilder functions except for arcs
private val NODE_ARC_PARAMETERS =
    listOf("horizontalEllipseRadius", "verticalEllipseRadius", "theta", "isMoreThanHalf", "isPositiveArc", "arcStartX", "arcStartY")
private val NODE_RELATIVE_ARC_PARAMETERS =
    listOf("a", "b", "theta", "isMoreThanHalf", "isPositiveArc", "arcStartDx", "arcStartDy")

private val COMPOSE_COLOR_CONSTANTS =
    mapOf(
        "Black" to Color(0xFF000000u),
        "DarkGray" to Color(0xFF444444u),
        "Gray" to Color(0xFF888888u),
        "LightGray" to Color(0xFFCCCCCCu),
        "White" to Color(0xFFFFFFFFu),
        "Red" to Color(0xFFFF0000u),
        "Green" to Color(0xFF00FF00u),
        "Blue" to Color(0xFF0000FFu),
        "Yellow" to Color(0xFFFFFF00u),
        "Cyan" to Color(0xFF00FFFFu),
        "Magenta" to Color(0xFFFF00FFu),
        "Transparent" to Color(0x00000000u),
    )

@ExperimentalVgoApi
fun parse(psiFile: KtFile): ImageVector {
    val vectors = findImageVectors(psiFile)

    return checkNotNull(vectors.firstOrNull()) {
        "Failed to parse ${psiFile.name}"
    }
}

/**
 * Find ImageVector definitions by locating ImageVector.Builder calls anywhere
 * in the file, regardless of the declaration shape surrounding them (property
 * getters, backing-property elvis expressions, lazy delegates, plain
 * initializers, or functions).
 */
private fun findImageVectors(file: KtFile): List<ImageVector> {
    val results = mutableListOf<ImageVector>()

    file.accept(
        object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                if (isImageVectorBuilderCall(expression, file)) {
                    val vector = parseImageVector(expression)
                    if (vector != null) {
                        results.add(vector)
                    }
                }
            }
        },
    )

    return results
}

private fun isImageVectorBuilderCall(
    call: KtCallExpression,
    file: KtFile,
): Boolean {
    if ((call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() != "Builder") return false

    val parent = call.parent
    if (parent is KtDotQualifiedExpression && parent.selectorExpression == call) {
        val receiverText = parent.receiverExpression.text
        return receiverText == "ImageVector" || receiverText.endsWith(".ImageVector")
    }

    // A bare Builder(...) call requires a direct import of ImageVector.Builder
    return file.importDirectives.any { it.importedFqName?.asString()?.endsWith("ImageVector.Builder") == true }
}

private fun parseImageVector(builderCall: KtCallExpression): ImageVector? {
    val arguments = resolveArguments(builderCall, BUILDER_PARAMETERS)

    val defaultWidthDp = parseDpValue(arguments["defaultWidth"]) ?: return null
    val defaultHeightDp = parseDpValue(arguments["defaultHeight"]) ?: return null
    val viewportWidth = parseFloatLiteral(arguments["viewportWidth"]) ?: return null
    val viewportHeight = parseFloatLiteral(arguments["viewportHeight"]) ?: return null

    return ImageVector(
        collectBuilderElements(builderCall),
        parseStringLiteral(arguments["name"]),
        deriveForeignKeys(builderCall),
        defaultWidthDp,
        defaultHeightDp,
        viewportWidth,
        viewportHeight,
    )
}

/**
 * The property or function name a vector definition belongs to survives
 * optimization via the foreign map so the writer can round-trip it.
 */
private fun deriveForeignKeys(builderCall: KtCallExpression): MutableMap<String, String> {
    val foreign = mutableMapOf<String, String>()

    var declaration: KtCallableDeclaration? =
        PsiTreeUtil.getParentOfType(builderCall, KtProperty::class.java, KtNamedFunction::class.java)
    while (declaration is KtProperty && declaration.isLocal) {
        declaration = PsiTreeUtil.getParentOfType(declaration, KtProperty::class.java, KtNamedFunction::class.java)
    }

    val name = declaration?.name ?: return foreign
    foreign[FOREIGN_KEY_PROPERTY_NAME] = name

    val receiverType = (declaration as? KtProperty)?.receiverTypeReference?.text
    if (receiverType != null) {
        foreign[FOREIGN_KEY_PACKAGE_NAME] = receiverType
    }

    return foreign
}

/**
 * Collect path and group elements applied to the builder, whether chained
 * fluently, applied inside a scope function like apply or run, or both.
 */
private fun collectBuilderElements(builderCall: KtCallExpression): List<Element> {
    val elements = mutableListOf<Element>()

    var current: KtExpression = builderCall
    (builderCall.parent as? KtDotQualifiedExpression)?.let { qualified ->
        if (qualified.selectorExpression == builderCall) current = qualified
    }

    while (true) {
        val parent = current.parent
        if (parent !is KtDotQualifiedExpression || parent.receiverExpression != current) break

        (parent.selectorExpression as? KtCallExpression)?.let { dispatchBuilderCall(it, elements) }
        current = parent
    }

    // Local variable form: val builder = ImageVector.Builder(...); builder.path { ... }; builder.build()
    val localProperty = current.parent as? KtProperty
    if (localProperty != null && localProperty.isLocal && localProperty.initializer == current) {
        val variableName = localProperty.name
        val block = localProperty.parent as? KtBlockExpression

        if (variableName != null && block != null) {
            for (statement in block.statements.dropWhile { it != localProperty }.drop(1)) {
                val qualified = statement as? KtDotQualifiedExpression ?: continue
                val receiver = qualified.receiverExpression as? KtNameReferenceExpression ?: continue
                if (receiver.getReferencedName() != variableName) continue

                (qualified.selectorExpression as? KtCallExpression)?.let { dispatchBuilderCall(it, elements) }
            }
        }
    }

    return elements
}

private fun dispatchBuilderCall(
    call: KtCallExpression,
    elements: MutableList<Element>,
) {
    when ((call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()) {
        "path", "group" -> parseElement(call)?.let(elements::add)
        "apply", "run" -> parseScopeFunctionBody(call, elements)
    }
}

private fun parseScopeFunctionBody(
    scopeCall: KtCallExpression,
    elements: MutableList<Element>,
) {
    val body =
        scopeCall.lambdaArguments
            .firstOrNull()
            ?.getLambdaExpression()
            ?.bodyExpression
            ?: (
                scopeCall.valueArgumentList
                    ?.arguments
                    ?.lastOrNull()
                    ?.getArgumentExpression() as? KtLambdaExpression
            )?.bodyExpression

    for (statement in body?.statements.orEmpty()) {
        when (statement) {
            is KtCallExpression -> {
                dispatchBuilderCall(statement, elements)
            }

            is KtDotQualifiedExpression -> {
                if (statement.receiverExpression is KtThisExpression) {
                    (statement.selectorExpression as? KtCallExpression)?.let { dispatchBuilderCall(it, elements) }
                }
            }
        }
    }
}

private fun parseElement(callExpression: KtCallExpression): Element? =
    when (callExpression.calleeExpression?.text) {
        "path" -> parsePathBuilderCall(callExpression)
        "group" -> parseGroupBuilderCall(callExpression)
        else -> null
    }

private fun parseGroupBuilderCall(callExpression: KtCallExpression): Group {
    val arguments = resolveArguments(callExpression, GROUP_PARAMETERS)

    val name = parseStringLiteral(arguments["name"])
    val clipPaths =
        parseClipPathDataExpression(arguments["clipPathData"])
            ?.let { listOf(ClipPath(regions = listOf(it))) }
            .orEmpty()

    val transform =
        computeTransformation(
            parseFloatLiteral(arguments["scaleX"]),
            parseFloatLiteral(arguments["scaleY"]),
            parseFloatLiteral(arguments["translationX"]),
            parseFloatLiteral(arguments["translationY"]),
            parseFloatLiteral(arguments["rotate"]),
            parseFloatLiteral(arguments["pivotX"]),
            parseFloatLiteral(arguments["pivotY"]),
        )

    val elements: List<Element> =
        callExpression.lambdaArguments.firstOrNull()?.let { lambdaArg ->
            val lambdaExpr = lambdaArg.getLambdaExpression()
            val bodyExpr = lambdaExpr?.bodyExpression

            val elements = mutableListOf<Element>()
            if (bodyExpr != null) {
                for (statement in bodyExpr.statements) {
                    if (statement is KtCallExpression) {
                        val element = parseElement(statement)
                        if (element != null) {
                            elements.add(element)
                        }
                    }
                }
            }

            elements
        } ?: emptyList()

    return Group(id = name, elements = elements, transform = transform, clipPaths = clipPaths, foreign = mutableMapOf())
}

private fun parsePathBuilderCall(callExpression: KtCallExpression): Path {
    val arguments = resolveArguments(callExpression, PATH_PARAMETERS, PATH_PARAMETER_ALIASES)

    // Process the path commands in the trailing lambda or the pathBuilder argument
    val bodyExpr =
        callExpression.lambdaArguments
            .firstOrNull()
            ?.getLambdaExpression()
            ?.bodyExpression
            ?: (arguments["pathBuilder"] as? KtLambdaExpression)?.bodyExpression

    val commands = bodyExpr?.let(::parsePathCommands).orEmpty()

    var effectiveFillColor = parseColorArgument(arguments["fill"]) ?: Colors.BLACK
    parseFloatLiteral(arguments["fillAlpha"])?.let {
        effectiveFillColor = effectiveFillColor.copy(alpha = (it * 255f).coerceIn(0f, 255f).toInt().toUByte())
    }
    var effectiveStrokeColor = parseColorArgument(arguments["stroke"]) ?: Colors.TRANSPARENT
    parseFloatLiteral(arguments["strokeAlpha"])?.let {
        effectiveStrokeColor = effectiveStrokeColor.copy(alpha = (it * 255f).coerceIn(0f, 255f).toInt().toUByte())
    }

    return Path(
        id = parseStringLiteral(arguments["name"]),
        commands = commands,
        fill = effectiveFillColor,
        stroke = effectiveStrokeColor,
        strokeWidth = parseFloatLiteral(arguments["strokeLineWidth"]) ?: 0f,
        fillRule = parseFillRule(arguments["pathFillType"]) ?: Path.FillRule.NON_ZERO,
        strokeLineCap = parseLineCap(arguments["strokeLineCap"]) ?: Path.LineCap.BUTT,
        strokeLineJoin = parseLineJoin(arguments["strokeLineJoin"]) ?: Path.LineJoin.MITER,
        strokeMiterLimit = parseFloatLiteral(arguments["strokeLineMiter"]) ?: 4f,
        foreign = mutableMapOf(),
    )
}

private fun parseFillRule(expression: KtExpression?): Path.FillRule? =
    when (lastSimpleName(expression)) {
        "EvenOdd" -> Path.FillRule.EVEN_ODD
        "NonZero" -> Path.FillRule.NON_ZERO
        else -> null
    }

private fun parseLineCap(expression: KtExpression?): Path.LineCap? =
    when (lastSimpleName(expression)) {
        "Butt" -> Path.LineCap.BUTT
        "Round" -> Path.LineCap.ROUND
        "Square" -> Path.LineCap.SQUARE
        else -> null
    }

private fun parseLineJoin(expression: KtExpression?): Path.LineJoin? =
    when (lastSimpleName(expression)) {
        "Miter" -> Path.LineJoin.MITER
        "Round" -> Path.LineJoin.ROUND
        "Bevel" -> Path.LineJoin.BEVEL
        else -> null
    }

private fun lastSimpleName(expression: KtExpression?): String? =
    when (expression) {
        is KtDotQualifiedExpression -> (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
        is KtNameReferenceExpression -> expression.getReferencedName()
        else -> null
    }

private fun parsePathCommands(bodyExpr: KtBlockExpression): List<Command> {
    val commands = mutableListOf<Command>()

    for (callExpression in bodyExpr.statements.filterIsInstance<KtCallExpression>()) {
        val command =
            when (callExpression.calleeExpression?.text) {
                "moveTo" -> {
                    parsePointArguments(callExpression, "x", "y")
                        ?.let { MoveTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "moveToRelative" -> {
                    parsePointArguments(callExpression, "dx", "dy")
                        ?.let { MoveTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "lineTo" -> {
                    parsePointArguments(callExpression, "x", "y")
                        ?.let { LineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "lineToRelative" -> {
                    parsePointArguments(callExpression, "dx", "dy")
                        ?.let { LineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "horizontalLineTo" -> {
                    parseFloatArgument(callExpression, "x")
                        ?.let { HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "horizontalLineToRelative" -> {
                    parseFloatArgument(callExpression, "dx")
                        ?.let { HorizontalLineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "verticalLineTo" -> {
                    parseFloatArgument(callExpression, "y")
                        ?.let { VerticalLineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "verticalLineToRelative" -> {
                    parseFloatArgument(callExpression, "dy")
                        ?.let { VerticalLineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "curveTo" -> {
                    parseCubicArgs(callExpression, CUBIC_PARAMETERS)
                        ?.let { CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "curveToRelative" -> {
                    parseCubicArgs(callExpression, RELATIVE_CUBIC_PARAMETERS)
                        ?.let { CubicBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "reflectiveCurveTo" -> {
                    parseReflectiveCubicArgs(callExpression, TWO_POINT_PARAMETERS)
                        ?.let { SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "reflectiveCurveToRelative" -> {
                    parseReflectiveCubicArgs(callExpression, RELATIVE_TWO_POINT_PARAMETERS)
                        ?.let { SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "quadTo" -> {
                    parseQuadArgs(callExpression, TWO_POINT_PARAMETERS)
                        ?.let { QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "quadToRelative" -> {
                    parseQuadArgs(callExpression, RELATIVE_TWO_POINT_PARAMETERS)
                        ?.let { QuadraticBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "reflectiveQuadTo" -> {
                    parsePointArguments(callExpression, "x1", "y1")
                        ?.let { SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "reflectiveQuadToRelative" -> {
                    parsePointArguments(callExpression, "dx1", "dy1")
                        ?.let { SmoothQuadraticBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "arcTo" -> {
                    parseArcArgument(callExpression, ARC_PARAMETERS)
                        ?.let { EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "arcToRelative" -> {
                    parseArcArgument(callExpression, RELATIVE_ARC_PARAMETERS)
                        ?.let { EllipticalArcCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "close" -> {
                    ClosePath
                }

                else -> {
                    null
                }
            }

        if (command != null) {
            commands.add(command)
        }
    }

    return commands
}

/**
 * Resolve a call's value arguments into a parameter-name-keyed map using the
 * callee's declared parameter order, honoring positional, named, and mixed usage.
 */
private fun resolveArguments(
    call: KtCallExpression,
    parameters: List<String>,
    aliases: Map<String, String> = emptyMap(),
): Map<String, KtExpression> {
    val resolved = mutableMapOf<String, KtExpression>()

    for ((index, argument) in call.valueArgumentList
        ?.arguments
        .orEmpty()
        .withIndex()) {
        val expression = argument.getArgumentExpression() ?: continue
        val name = argument.getArgumentName()?.asName?.identifier
        val parameter =
            if (name != null) {
                aliases[name] ?: name
            } else {
                parameters.getOrNull(index) ?: continue
            }
        resolved[parameter] = expression
    }

    return resolved
}

private fun parseStringLiteral(expression: KtExpression?): String? {
    val template = expression as? KtStringTemplateExpression ?: return null

    return template.entries.joinToString("") { entry ->
        when (entry) {
            is KtLiteralStringTemplateEntry -> entry.text
            is KtSimpleNameStringTemplateEntry -> entry.text
            else -> ""
        }
    }
}

private fun parseDpValue(expression: KtExpression?): Float? =
    when {
        expression is KtDotQualifiedExpression &&
            (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() == "dp" -> {
            parseFloatLiteral(expression.receiverExpression)
        }

        expression is KtCallExpression && expression.calleeExpression?.text == "Dp" -> {
            parseFloatLiteral(
                expression.valueArgumentList
                    ?.arguments
                    ?.firstOrNull()
                    ?.getArgumentExpression(),
            )
        }

        else -> {
            parseFloatLiteral(expression)
        }
    }

private fun parseFloatLiteral(expression: KtExpression?): Float? {
    if (expression == null) return null

    return (expression as? KtConstantExpression)?.text?.toFloatOrNull()
        ?: (expression as? KtConstantExpression)?.text?.removeSuffix("f")?.toFloatOrNull()
        ?: (expression as? KtPrefixExpression)?.let {
            val name =
                it.baseExpression
                    ?.text
                    ?.removeSuffix("f")
                    ?.toFloatOrNull()
            if (it.operationToken.toString() == "MINUS" && name != null) {
                -name
            } else {
                name
            }
        }
}

private fun parseBooleanArgument(expression: KtExpression?): Boolean? {
    if (!KtPsiUtil.isBooleanConstant(expression)) return null

    return KtPsiUtil.isTrueConstant(expression)
}

private fun parseFloats(
    call: KtCallExpression,
    parameters: List<String>,
): List<Float>? {
    val arguments = resolveArguments(call, parameters)

    val values = ArrayList<Float>(parameters.size)
    for (parameter in parameters) {
        values.add(parseFloatLiteral(arguments[parameter]) ?: return null)
    }

    return values
}

private fun parseFloatArgument(
    call: KtCallExpression,
    parameter: String,
): Float? = parseFloats(call, listOf(parameter))?.first()

private fun parsePointArguments(
    call: KtCallExpression,
    xParameter: String,
    yParameter: String,
): Point? = parseFloats(call, listOf(xParameter, yParameter))?.let { Point(it[0], it[1]) }

private fun parseCubicArgs(
    call: KtCallExpression,
    parameters: List<String>,
): CubicBezierCurve.Parameter? =
    parseFloats(call, parameters)?.let {
        CubicBezierCurve.Parameter(Point(it[0], it[1]), Point(it[2], it[3]), Point(it[4], it[5]))
    }

private fun parseQuadArgs(
    call: KtCallExpression,
    parameters: List<String>,
): QuadraticBezierCurve.Parameter? =
    parseFloats(call, parameters)?.let {
        QuadraticBezierCurve.Parameter(Point(it[0], it[1]), Point(it[2], it[3]))
    }

private fun parseReflectiveCubicArgs(
    call: KtCallExpression,
    parameters: List<String>,
): SmoothCubicBezierCurve.Parameter? =
    parseFloats(call, parameters)?.let {
        SmoothCubicBezierCurve.Parameter(Point(it[0], it[1]), Point(it[2], it[3]))
    }

private fun parseArcArgument(
    call: KtCallExpression,
    parameters: List<String>,
): EllipticalArcCurve.Parameter? {
    val arguments = resolveArguments(call, parameters)

    val radiusX = parseFloatLiteral(arguments[parameters[0]]) ?: return null
    val radiusY = parseFloatLiteral(arguments[parameters[1]]) ?: return null
    val angle = parseFloatLiteral(arguments[parameters[2]]) ?: return null
    val isMoreThanHalf = parseBooleanArgument(arguments[parameters[3]]) ?: return null
    val isPositiveArc = parseBooleanArgument(arguments[parameters[4]]) ?: return null
    val endX = parseFloatLiteral(arguments[parameters[5]]) ?: return null
    val endY = parseFloatLiteral(arguments[parameters[6]]) ?: return null

    return EllipticalArcCurve.Parameter(
        radiusX,
        radiusY,
        angle,
        if (isMoreThanHalf) EllipticalArcCurve.ArcFlag.LARGE else EllipticalArcCurve.ArcFlag.SMALL,
        if (isPositiveArc) EllipticalArcCurve.SweepFlag.CLOCKWISE else EllipticalArcCurve.SweepFlag.ANTICLOCKWISE,
        Point(endX, endY),
    )
}

private fun parseColorArgument(expression: KtExpression?): Color? {
    var colorExpression = expression ?: return null

    // Unwrap SolidColor(...)
    if (colorExpression is KtCallExpression && colorExpression.calleeExpression?.text == "SolidColor") {
        colorExpression = colorExpression.valueArgumentList
            ?.arguments
            ?.firstOrNull()
            ?.getArgumentExpression() ?: return null
    }

    // Handle companion constants like Color.Black
    if (colorExpression is KtDotQualifiedExpression &&
        colorExpression.receiverExpression.text.substringAfterLast('.') == "Color"
    ) {
        val constant = (colorExpression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
        return COMPOSE_COLOR_CONSTANTS[constant]
    }

    if (colorExpression is KtCallExpression && colorExpression.calleeExpression?.text == "Color") {
        val arguments = colorExpression.valueArgumentList?.arguments.orEmpty()

        // Handle Color(0xFF232F34)
        if (arguments.size == 1) {
            return parseColorLiteral(arguments[0].getArgumentExpression())
        }

        // Handle Color(r, g, b) and Color(r, g, b, a) with integer channels
        if (arguments.size >= 3) {
            val channels = resolveArguments(colorExpression, listOf("red", "green", "blue", "alpha"))
            val red = channels["red"]?.text?.toUByteOrNull() ?: return null
            val green = channels["green"]?.text?.toUByteOrNull() ?: return null
            val blue = channels["blue"]?.text?.toUByteOrNull() ?: return null
            val alpha = channels["alpha"]?.text?.toUByteOrNull() ?: 0xFF.toUByte()
            return Color(alpha, red, green, blue)
        }
    }

    return null
}

private fun parseColorLiteral(expression: KtExpression?): Color? {
    val text =
        (expression as? KtConstantExpression)
            ?.text
            ?.replace("_", "")
            ?.removeSuffix("L") ?: return null

    val argb =
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text.drop(2).toUIntOrNull(16)
        } else {
            text.toUIntOrNull()
        }

    return argb?.let(::Color)
}

private fun parseClipPathDataExpression(expression: KtExpression?): Path? {
    val listOfCall = expression as? KtCallExpression ?: return null
    if (listOfCall.calleeExpression?.text != "listOf") return null

    val commands = parseClipPathNodes(listOfCall)
    if (commands.isEmpty()) return null

    return Path(
        id = null,
        commands = commands,
        fill = Colors.BLACK,
        stroke = Colors.TRANSPARENT,
        strokeWidth = 0f,
        fillRule = Path.FillRule.NON_ZERO,
        strokeLineCap = Path.LineCap.BUTT,
        strokeLineJoin = Path.LineJoin.MITER,
        strokeMiterLimit = 4f,
        foreign = mutableMapOf(),
    )
}

private fun parseClipPathNodes(listOfCall: KtCallExpression): List<Command> {
    val commands = mutableListOf<Command>()

    for (arg in listOfCall.valueArgumentList?.arguments.orEmpty()) {
        val node = arg.getArgumentExpression() as? KtDotQualifiedExpression ?: continue
        if (node.receiverExpression.text != "PathNode") continue
        val selector = node.selectorExpression
        if (selector is KtNameReferenceExpression && selector.text == "Close") {
            commands.add(ClosePath)
            continue
        }
        val call = selector as? KtCallExpression ?: continue
        val command =
            when (call.calleeExpression?.text) {
                "MoveTo" -> {
                    parsePointArguments(call, "x", "y")
                        ?.let { MoveTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeMoveTo" -> {
                    parsePointArguments(call, "dx", "dy")
                        ?.let { MoveTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "LineTo" -> {
                    parsePointArguments(call, "x", "y")
                        ?.let { LineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeLineTo" -> {
                    parsePointArguments(call, "dx", "dy")
                        ?.let { LineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "HorizontalTo" -> {
                    parseFloatArgument(call, "x")
                        ?.let { HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeHorizontalTo" -> {
                    parseFloatArgument(call, "dx")
                        ?.let { HorizontalLineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "VerticalTo" -> {
                    parseFloatArgument(call, "y")
                        ?.let { VerticalLineTo(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeVerticalTo" -> {
                    parseFloatArgument(call, "dy")
                        ?.let { VerticalLineTo(CommandVariant.RELATIVE, listOf(it)) }
                }

                "CurveTo" -> {
                    parseCubicArgs(call, CUBIC_PARAMETERS)
                        ?.let { CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeCurveTo" -> {
                    parseCubicArgs(call, RELATIVE_CUBIC_PARAMETERS)
                        ?.let { CubicBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "ReflectiveCurveTo" -> {
                    parseReflectiveCubicArgs(call, TWO_POINT_PARAMETERS)
                        ?.let { SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeReflectiveCurveTo" -> {
                    parseReflectiveCubicArgs(call, RELATIVE_TWO_POINT_PARAMETERS)
                        ?.let { SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "QuadTo" -> {
                    parseQuadArgs(call, TWO_POINT_PARAMETERS)
                        ?.let { QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeQuadTo" -> {
                    parseQuadArgs(call, RELATIVE_TWO_POINT_PARAMETERS)
                        ?.let { QuadraticBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "ReflectiveQuadTo" -> {
                    parsePointArguments(call, "x", "y")
                        ?.let { SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeReflectiveQuadTo" -> {
                    parsePointArguments(call, "dx", "dy")
                        ?.let { SmoothQuadraticBezierCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                "ArcTo" -> {
                    parseArcArgument(call, NODE_ARC_PARAMETERS)
                        ?.let { EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(it)) }
                }

                "RelativeArcTo" -> {
                    parseArcArgument(call, NODE_RELATIVE_ARC_PARAMETERS)
                        ?.let { EllipticalArcCurve(CommandVariant.RELATIVE, listOf(it)) }
                }

                else -> {
                    null
                }
            }

        if (command != null) {
            commands.add(command)
        }
    }

    return commands
}
