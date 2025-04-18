@file:OptIn(ExperimentalVgoApi::class)

package com.jzbrooks.vgo.iv

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Group
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.core.util.math.Point
import com.jzbrooks.vgo.core.util.math.computeTransformation
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument

@ExperimentalVgoApi
fun parse(psiFile: KtFile): ImageVector {
    val propertyVectors = findPropertyVectors(psiFile)

    return checkNotNull(propertyVectors.firstOrNull()) {
        "Failed to parse ${psiFile.name}"
    }
}

/**
 * Find property-based ImageVector definitions like:
 * val BackingIcons.Outlined.Add: ImageVector get() = _Add ?: ImageVector.Builder(...).apply { ... }.build()
 */
private fun findPropertyVectors(file: KtFile): List<ImageVector> {
    val results = mutableListOf<ImageVector>()

    file.accept(
        object : KtTreeVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)

                // Check if it's an ImageVector property
                if (property.typeReference?.text == "ImageVector") {
                    val getter = property.getter
                    if (getter != null) {
                        val vectorGraphic = parsePropertyGetter(property, getter)
                        if (vectorGraphic != null) {
                            results.add(vectorGraphic)
                        }
                    }
                }
            }
        },
    )

    return results
}

/**
 * Parse the getter of an ImageVector property to extract the vector definition
 */
private fun parsePropertyGetter(
    property: KtProperty,
    getter: KtPropertyAccessor,
): ImageVector? {
    val propertyName = property.name ?: return null

    // Find package name from qualified property if available
    val packageName =
        if (property.receiverTypeReference != null) {
            property.receiverTypeReference!!.text
        } else {
            null
        }

    // Extract the body of the getter
    val bodyExpr = getter.bodyExpression ?: return null

    // Handle the _Add ?: ImageVector.Builder(...).apply { ... }.build() pattern
    var builderExpression: KtExpression? = null

    // First check if there's a direct return expression
    if (bodyExpr is KtBlockExpression) {
        for (statement in bodyExpr.statements) {
            if (statement is KtReturnExpression) {
                val returnValue = statement.returnedExpression
                builderExpression =
                    if (returnValue is KtBinaryExpression && returnValue.operationToken.toString() == "ELVIS") {
                        // Handle the elvis operator (_Add ?: ImageVector.Builder...)
                        returnValue.right
                    } else {
                        // Direct return of builder expression
                        returnValue
                    }
                break
            } else if (statement is KtBinaryExpression && statement.operationToken.toString() == "ELVIS") {
                // Handle the elvis operator without explicit return
                builderExpression = statement.right
                break
            } else if (statement is KtDotQualifiedExpression) {
                // Direct builder expression without elvis
                builderExpression = statement
                break
            }
        }
    } else if (bodyExpr is KtBinaryExpression && bodyExpr.operationToken.toString() == "ELVIS") {
        // Handle elvis operator as the direct body
        builderExpression = bodyExpr.right
    } else {
        // Direct expression body
        builderExpression = bodyExpr
    }

    if (builderExpression != null) {
        // If the expression ends with .build().also { _Add = it }
        if (builderExpression is KtDotQualifiedExpression &&
            builderExpression.selectorExpression?.text?.contains("also") == true
        ) {
            // Extract the part before .also
            builderExpression = builderExpression.receiverExpression
        }

        // If the expression ends with .build()
        if (builderExpression is KtDotQualifiedExpression &&
            builderExpression.selectorExpression?.text?.contains("build") == true
        ) {
            // Extract the part before .build()
            builderExpression = builderExpression.receiverExpression
        }

        // Now we should have the calls up to .build()
        return parseVectorBuilderExpression(builderExpression, propertyName, packageName)
    }

    return null
}

private fun parseVectorBuilderExpression(
    expression: KtExpression,
    propertyName: String,
    packageName: String?,
): ImageVector? {
    val elements = mutableListOf<Element>()
    var id: String? = null
    val foreign = mutableMapOf<String, String>()

    if (expression is KtDotQualifiedExpression) {
        // Extract the Builder part
        val builderExpr = expression.receiverExpression

        if (builderExpr is KtCallExpression ||
            (builderExpr is KtDotQualifiedExpression && builderExpr.selectorExpression is KtCallExpression)
        ) {
            // Extract the builder arguments (name, dimensions, etc.)
            val callExpr =
                builderExpr as? KtCallExpression
                    ?: PsiTreeUtil.findChildOfType(builderExpr, KtCallExpression::class.java)!!

            callExpr.valueArgumentList?.arguments?.forEach { arg ->
                if (arg is KtValueArgument) {
                    val argumentName = arg.getArgumentName()?.asName?.identifier
                    val argExpr = arg.getArgumentExpression()

                    when (argumentName) {
                        "name" -> {
                            if (argExpr is KtStringTemplateExpression) {
                                id =
                                    argExpr.entries.joinToString("") {
                                        when (it) {
                                            is KtLiteralStringTemplateEntry -> it.text
                                            is KtSimpleNameStringTemplateEntry -> it.text
                                            else -> ""
                                        }
                                    }
                            }
                        }

                        "defaultWidth", "defaultHeight", "viewportWidth", "viewportHeight" -> {
                            if (argExpr != null) {
                                foreign[argumentName] = argExpr.text.removeSuffix(".dp").removeSuffix("f")
                            }
                        }
                    }
                }
            }

            var callExpression = (callExpr.parent.parent as? KtDotQualifiedExpression)?.selectorExpression as? KtCallExpression
            while (callExpression != null && callExpression.name != "build") {
                val element = parseElement(callExpression)
                if (element != null) elements.add(element)

                callExpression = (callExpression.parent.parent as? KtDotQualifiedExpression)?.selectorExpression as? KtCallExpression
            }
        }
    }

    return if (elements.isNotEmpty() || id != null) {
        ImageVector(elements, id, foreign, propertyName, packageName)
    } else {
        null
    }
}

private fun parseElement(callExpression: KtCallExpression): Element? =
    when (callExpression.calleeExpression?.text) {
        "path" -> parsePathBuilderCall(callExpression)
        "group" -> parseGroupBuilderCall(callExpression)
        else -> null
    }

private fun parseGroupBuilderCall(callExpression: KtCallExpression): Group? {
    var name: String? = null
    var rotation: Float? = null
    var pivotX: Float? = null
    var pivotY: Float? = null
    var scaleX: Float? = null
    var scaleY: Float? = null
    var translationX: Float? = null
    var translationY: Float? = null

    callExpression.valueArgumentList?.arguments?.forEach { arg ->
        if (arg is KtValueArgument) {
            val argumentName = arg.getArgumentName()?.asName?.identifier
            val expression = arg.getArgumentExpression()

            when (argumentName) {
                "name" -> {
                    if (expression is KtStringTemplateExpression && expression.entries.size == 1) {
                        name =
                            when (val literalExpr = expression.entries.first()) {
                                is KtLiteralStringTemplateEntry -> literalExpr.text
                                is KtSimpleNameStringTemplateEntry -> literalExpr.text
                                else -> ""
                            }
                    }
                }

                "rotate" -> {
                    rotation = parseFloatLiteral(expression)
                }

                "pivotX" -> {
                    pivotX = parseFloatLiteral(expression)
                }

                "pivotY" -> {
                    pivotY = parseFloatLiteral(expression)
                }

                "scaleX" -> {
                    scaleX = parseFloatLiteral(expression)
                }

                "scaleY" -> {
                    scaleY = parseFloatLiteral(expression)
                }

                "translationX" -> {
                    translationX = parseFloatLiteral(expression)
                }

                "translationY" -> {
                    translationY = parseFloatLiteral(expression)
                }
            }
        }
    }

    val transform = computeTransformation(scaleX, scaleY, translationX, translationY, rotation, pivotX, pivotY)

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

    return Group(id = name, elements = elements, transform = transform, foreign = mutableMapOf())
}

private fun parsePathBuilderCall(callExpression: KtCallExpression): Path? {
    var fillColor: Color? = null
    var strokeColor: Color? = null
    var strokeWidth: Float? = null
    var fillAlpha: Float? = null
    var strokeAlpha: Float? = null
    val commands = mutableListOf<Command>()

    // Extract named arguments from path call
    callExpression.valueArgumentList?.arguments?.forEach { arg ->
        if (arg is KtValueArgument) {
            val argumentName = arg.getArgumentName()?.asName?.identifier
            val expression = arg.getArgumentExpression()

            when (argumentName) {
                "fill" -> {
                    fillColor = parseColorArgument(expression)
                }
                "stroke" -> {
                    strokeColor = parseColorArgument(expression)
                }
                "strokeWidth", "strokeLineWidth" -> {
                    val floatText = (expression as? KtConstantExpression)?.text
                    strokeWidth = floatText?.toFloatOrNull()
                        ?: floatText?.removeSuffix("f")?.toFloatOrNull()
                }
                "fillAlpha" -> {
                    val floatText = (expression as? KtConstantExpression)?.text
                    fillAlpha = floatText?.toFloatOrNull()
                        ?: floatText?.removeSuffix("f")?.toFloatOrNull()
                }
                "strokeAlpha" -> {
                    val floatText = (expression as? KtConstantExpression)?.text
                    strokeAlpha = floatText?.toFloatOrNull()
                        ?: floatText?.removeSuffix("f")?.toFloatOrNull()
                }
            }
        }
    }

    // Process the path commands in the lambda block
    val lambdaArg = callExpression.lambdaArguments.firstOrNull()
    val lambdaExpr = lambdaArg?.getLambdaExpression()
    val bodyExpr = lambdaExpr?.bodyExpression

    if (bodyExpr != null) {
        commands.addAll(parsePathCommands(bodyExpr))
    }

    var effectiveFillColor = fillColor ?: Colors.BLACK
    fillAlpha?.let {
        effectiveFillColor = effectiveFillColor.copy(alpha = (it * 255f).coerceIn(0f, 255f).toInt().toUByte())
    }
    var effectiveStrokeColor = strokeColor ?: Colors.TRANSPARENT
    strokeAlpha?.let {
        effectiveStrokeColor = effectiveStrokeColor.copy(alpha = (it * 255f).coerceIn(0f, 255f).toInt().toUByte())
    }

    return Path(
        id = null,
        commands = commands,
        fill = effectiveFillColor,
        stroke = effectiveStrokeColor,
        strokeWidth = strokeWidth ?: 0f,
        fillRule = Path.FillRule.EVEN_ODD,
        strokeLineCap = Path.LineCap.BUTT,
        strokeLineJoin = Path.LineJoin.MITER,
        strokeMiterLimit = 4f,
        foreign = mutableMapOf(),
    )
}

private fun parsePathCommands(bodyExpr: KtBlockExpression): List<Command> {
    val commands = mutableListOf<Command>()

    // Process each path command in the block
    for (statement in bodyExpr.statements) {
        if (statement is KtCallExpression) {
            val commandName = statement.calleeExpression?.text
            when (commandName) {
                "moveTo" -> {
                    val point = parsePointArguments(statement)
                    if (point != null) {
                        commands.add(MoveTo(CommandVariant.ABSOLUTE, listOf(point)))
                    }
                }
                "lineTo" -> {
                    val point = parsePointArguments(statement)
                    if (point != null) {
                        commands.add(LineTo(CommandVariant.ABSOLUTE, listOf(point)))
                    }
                }
                "lineToRelative" -> {
                    val point = parsePointArguments(statement)
                    if (point != null) {
                        commands.add(LineTo(CommandVariant.RELATIVE, listOf(point)))
                    }
                }
                "horizontalLineTo" -> {
                    val point = parseFloatArgument(statement)
                    if (point != null) {
                        commands.add(HorizontalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                    }
                }
                "horizontalLineToRelative" -> {
                    val point = parseFloatArgument(statement)
                    if (point != null) {
                        commands.add(HorizontalLineTo(CommandVariant.RELATIVE, listOf(point)))
                    }
                }
                "verticalLineTo" -> {
                    val point = parseFloatArgument(statement)
                    if (point != null) {
                        commands.add(VerticalLineTo(CommandVariant.ABSOLUTE, listOf(point)))
                    }
                }
                "verticalLineToRelative" -> {
                    val point = parseFloatArgument(statement)
                    if (point != null) {
                        commands.add(VerticalLineTo(CommandVariant.RELATIVE, listOf(point)))
                    }
                }
                "curveTo" -> {
                    val point = parseCubicArgs(statement)
                    if (point != null) {
                        commands.add(CubicBezierCurve(CommandVariant.ABSOLUTE, listOf(point)))
                    }
                }
                "curveToRelative" -> {
                    val point = parseCubicArgs(statement)
                    if (point != null) {
                        commands.add(CubicBezierCurve(CommandVariant.RELATIVE, listOf(point)))
                    }
                }
                // todo: quad
                // todo: arc
                "close" -> {
                    commands.add(ClosePath)
                }
            }
        }
    }

    return commands
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

private fun parseColorArgument(expression: KtExpression?): Color? {
    if (expression == null) return null

    // Handle SolidColor(Color(0xFF232F34))
    if (expression is KtCallExpression && expression.calleeExpression?.text == "SolidColor") {
        val colorArg =
            expression.valueArgumentList
                ?.arguments
                ?.firstOrNull()
                ?.getArgumentExpression()
        if (colorArg is KtCallExpression && colorArg.calleeExpression?.text == "Color") {
            val colorValueArgs = colorArg.valueArgumentList?.arguments

            if (colorValueArgs != null && colorValueArgs.size == 1) {
                val colorValueArg = colorValueArgs[0].getArgumentExpression()
                if (colorValueArg is KtConstantExpression) {
                    return colorValueArg.text.toUIntOrNull()?.let(::Color)
                }
            } else if (colorValueArgs != null && colorValueArgs.size == 4) {
                val r = colorValueArgs[0].getArgumentExpression()?.text?.toUByteOrNull() ?: return null
                val g = colorValueArgs[1].getArgumentExpression()?.text?.toUByteOrNull() ?: return null
                val b = colorValueArgs[2].getArgumentExpression()?.text?.toUByteOrNull() ?: return null
                val a = colorValueArgs[3].getArgumentExpression()?.text?.toUByteOrNull() ?: return null
                return Color(a, r, g, b)
            }
        }
    }

    // Handle direct Color(0xFF232F34)
    if (expression is KtCallExpression && expression.calleeExpression?.text == "Color") {
        val colorValueArg =
            expression.valueArgumentList
                ?.arguments
                ?.firstOrNull()
                ?.getArgumentExpression()
        if (colorValueArg is KtConstantExpression) {
            return colorValueArg.text.toUIntOrNull()?.let(::Color)
        }
    }

    return null
}
