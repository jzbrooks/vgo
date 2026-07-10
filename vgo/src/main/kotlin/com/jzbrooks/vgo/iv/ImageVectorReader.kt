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
        val commandName = callExpression.calleeExpression?.text
        when (commandName) {
            "moveTo" -> {
                val point = parsePointArguments(callExpression)
                if (point != null) {
                    commands.add(MoveTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "moveToRelative" -> {
                val point = parsePointArguments(callExpression)
                if (point != null) {
                    commands.add(MoveTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "lineTo" -> {
                val point = parsePointArguments(callExpression)
                if (point != null) {
                    commands.add(LineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "lineToRelative" -> {
                val point = parsePointArguments(callExpression)
                if (point != null) {
                    commands.add(LineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "horizontalLineTo" -> {
                val point = parseFloatArgument(callExpression)
                if (point != null) {
                    commands.add(HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "horizontalLineToRelative" -> {
                val point = parseFloatArgument(callExpression)
                if (point != null) {
                    commands.add(HorizontalLineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "verticalLineTo" -> {
                val point = parseFloatArgument(callExpression)
                if (point != null) {
                    commands.add(VerticalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "verticalLineToRelative" -> {
                val point = parseFloatArgument(callExpression)
                if (point != null) {
                    commands.add(VerticalLineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "curveTo" -> {
                val arg = parseCubicArgs(callExpression)
                if (arg != null) {
                    commands.add(CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "curveToRelative" -> {
                val arg = parseCubicArgs(callExpression)
                if (arg != null) {
                    commands.add(CubicBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "reflectiveCurveTo" -> {
                val arg = parseReflectiveCubicArgs(callExpression)
                if (arg != null) {
                    commands.add(SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "reflectiveCurveToRelative" -> {
                val arg = parseReflectiveCubicArgs(callExpression)
                if (arg != null) {
                    commands.add(SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "quadTo" -> {
                val arg = parseQuadArgs(callExpression)
                if (arg != null) {
                    commands.add(QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "quadToRelative" -> {
                val arg = parseQuadArgs(callExpression)
                if (arg != null) {
                    commands.add(QuadraticBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "reflectiveQuadTo" -> {
                val arg = parsePointArguments(callExpression)
                if (arg != null) {
                    commands.add(SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "reflectiveQuadToRelative" -> {
                val arg = parsePointArguments(callExpression)
                if (arg != null) {
                    commands.add(SmoothQuadraticBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "arcTo" -> {
                val arg = parseArcArgument(callExpression)
                if (arg != null) {
                    commands.add(EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "arcToRelative" -> {
                val arg = parseArcArgument(callExpression)
                if (arg != null) {
                    commands.add(EllipticalArcCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "close" -> {
                commands.add(ClosePath)
            }
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

private fun parseFloatArgument(callExpression: KtCallExpression): Float? {
    val args = callExpression.valueArgumentList?.arguments ?: return null

    val xArg = args[0].getArgumentExpression()

    return parseFloatLiteral(xArg)
}

private fun parsePointArguments(callExpression: KtCallExpression): Point? {
    val args = callExpression.valueArgumentList?.arguments ?: return null

    if (args.size >= 2) {
        val xArg = args[0].getArgumentExpression()
        val yArg = args[1].getArgumentExpression()

        val x = parseFloatLiteral(xArg) ?: return null
        val y = parseFloatLiteral(yArg) ?: return null

        return Point(x, y)
    }

    return null
}

private fun parseCubicArgs(callExpression: KtCallExpression): CubicBezierCurve.Parameter? {
    val args = callExpression.valueArgumentList?.arguments ?: return null

    if (args.size >= 6) {
        val startControlX = args[0].getArgumentExpression()
        val startControlY = args[1].getArgumentExpression()
        val endControlX = args[2].getArgumentExpression()
        val endControlY = args[3].getArgumentExpression()
        val endX = args[4].getArgumentExpression()
        val endY = args[5].getArgumentExpression()

        return CubicBezierCurve.Parameter(
            Point(
                parseFloatLiteral(startControlX) ?: return null,
                parseFloatLiteral(startControlY) ?: return null,
            ),
            Point(
                parseFloatLiteral(endControlX) ?: return null,
                parseFloatLiteral(endControlY) ?: return null,
            ),
            Point(
                parseFloatLiteral(endX) ?: return null,
                parseFloatLiteral(endY) ?: return null,
            ),
        )
    }

    return null
}

private fun parseQuadArgs(callExpression: KtCallExpression): QuadraticBezierCurve.Parameter? {
    val args = callExpression.valueArgumentList?.arguments ?: return null

    if (args.size >= 4) {
        val controlX = args[0].getArgumentExpression()
        val controlY = args[1].getArgumentExpression()
        val endX = args[2].getArgumentExpression()
        val endY = args[3].getArgumentExpression()

        return QuadraticBezierCurve.Parameter(
            Point(
                parseFloatLiteral(controlX) ?: return null,
                parseFloatLiteral(controlY) ?: return null,
            ),
            Point(
                parseFloatLiteral(endX) ?: return null,
                parseFloatLiteral(endY) ?: return null,
            ),
        )
    }

    return null
}

private fun parseReflectiveCubicArgs(callExpression: KtCallExpression): SmoothCubicBezierCurve.Parameter? {
    val args = callExpression.valueArgumentList?.arguments ?: return null

    if (args.size >= 4) {
        val endControlX = args[0].getArgumentExpression()
        val endControlY = args[1].getArgumentExpression()
        val endX = args[2].getArgumentExpression()
        val endY = args[3].getArgumentExpression()

        return SmoothCubicBezierCurve.Parameter(
            Point(
                parseFloatLiteral(endControlX) ?: return null,
                parseFloatLiteral(endControlY) ?: return null,
            ),
            Point(
                parseFloatLiteral(endX) ?: return null,
                parseFloatLiteral(endY) ?: return null,
            ),
        )
    }

    return null
}

private fun parseArcArgument(expression: KtCallExpression): EllipticalArcCurve.Parameter? {
    val args = expression.valueArgumentList?.arguments ?: return null

    if (args.size >= 7) {
        val radiusX = args[0].getArgumentExpression()
        val radiusY = args[1].getArgumentExpression()
        val xAxisRotation = args[2].getArgumentExpression()
        val isMoreThanHalf = parseBooleanArgument(args[3].getArgumentExpression()) ?: return null
        val isPositiveArc = parseBooleanArgument(args[4].getArgumentExpression()) ?: return null
        val endX = args[5].getArgumentExpression()
        val endY = args[6].getArgumentExpression()

        return EllipticalArcCurve.Parameter(
            parseFloatLiteral(radiusX) ?: return null,
            parseFloatLiteral(radiusY) ?: return null,
            parseFloatLiteral(xAxisRotation) ?: return null,
            if (isMoreThanHalf) EllipticalArcCurve.ArcFlag.LARGE else EllipticalArcCurve.ArcFlag.SMALL,
            if (isPositiveArc) EllipticalArcCurve.SweepFlag.CLOCKWISE else EllipticalArcCurve.SweepFlag.ANTICLOCKWISE,
            Point(
                parseFloatLiteral(endX) ?: return null,
                parseFloatLiteral(endY) ?: return null,
            ),
        )
    }

    return null
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
        when (call.calleeExpression?.text) {
            "MoveTo" -> {
                val point = parsePointArguments(call)
                if (point != null) {
                    commands.add(MoveTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "RelativeMoveTo" -> {
                val point = parsePointArguments(call)
                if (point != null) {
                    commands.add(MoveTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "LineTo" -> {
                val point = parsePointArguments(call)
                if (point != null) {
                    commands.add(LineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "RelativeLineTo" -> {
                val point = parsePointArguments(call)
                if (point != null) {
                    commands.add(LineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "HorizontalTo" -> {
                val point = parseFloatArgument(call)
                if (point != null) {
                    commands.add(HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "RelativeHorizontalTo" -> {
                val point = parseFloatArgument(call)
                if (point != null) {
                    commands.add(HorizontalLineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "VerticalTo" -> {
                val point = parseFloatArgument(call)
                if (point != null) {
                    commands.add(VerticalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                }
            }

            "RelativeVerticalTo" -> {
                val point = parseFloatArgument(call)
                if (point != null) {
                    commands.add(VerticalLineTo(CommandVariant.RELATIVE, listOf(point)))
                }
            }

            "CurveTo" -> {
                val arg = parseCubicArgs(call)
                if (arg != null) {
                    commands.add(CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "RelativeCurveTo" -> {
                val arg = parseCubicArgs(call)
                if (arg != null) {
                    commands.add(CubicBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "ReflectiveCurveTo" -> {
                val arg = parseReflectiveCubicArgs(call)
                if (arg != null) {
                    commands.add(SmoothCubicBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "RelativeReflectiveCurveTo" -> {
                val arg = parseReflectiveCubicArgs(call)
                if (arg != null) {
                    commands.add(SmoothCubicBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "QuadTo" -> {
                val arg = parseQuadArgs(call)
                if (arg != null) {
                    commands.add(QuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "RelativeQuadTo" -> {
                val arg = parseQuadArgs(call)
                if (arg != null) {
                    commands.add(QuadraticBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "ReflectiveQuadTo" -> {
                val arg = parsePointArguments(call)
                if (arg != null) {
                    commands.add(SmoothQuadraticBezierCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "RelativeReflectiveQuadTo" -> {
                val arg = parsePointArguments(call)
                if (arg != null) {
                    commands.add(SmoothQuadraticBezierCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }

            "ArcTo" -> {
                val arg = parseArcArgument(call)
                if (arg != null) {
                    commands.add(EllipticalArcCurve(CommandVariant.ABSOLUTE, listOf(arg)))
                }
            }

            "RelativeArcTo" -> {
                val arg = parseArcArgument(call)
                if (arg != null) {
                    commands.add(EllipticalArcCurve(CommandVariant.RELATIVE, listOf(arg)))
                }
            }
        }
    }

    return commands
}
