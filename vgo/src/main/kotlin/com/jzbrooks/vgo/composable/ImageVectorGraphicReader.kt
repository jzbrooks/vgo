package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.Colors
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.ClosePath
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.graphic.command.CommandVariant
import com.jzbrooks.vgo.core.graphic.command.CubicBezierCurve
import com.jzbrooks.vgo.core.graphic.command.HorizontalLineTo
import com.jzbrooks.vgo.core.graphic.command.LineTo
import com.jzbrooks.vgo.core.graphic.command.MoveTo
import com.jzbrooks.vgo.core.graphic.command.VerticalLineTo
import com.jzbrooks.vgo.core.util.math.Point
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import java.io.File

fun parse(file: File): ImageVectorGraphic {
    val text = file.readText()
    val disposable = Disposer.newDisposable()

    try {
        val configuration = CompilerConfiguration()
        configuration.put(
            CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY,
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false),
        )

        val environment =
            KotlinCoreEnvironment.createForProduction(
                disposable,
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES,
            )

        val propertyNameFallback = file.nameWithoutExtension
        val project = environment.project
        val psiFile = createPsiFile(project, file.name, text)

        // Find and parse property-based vector definitions
        val propertyVectors = findPropertyVectors(psiFile)
        if (propertyVectors.isNotEmpty()) {
            val first = propertyVectors.first()
            return first
        }

        // Fallback to the older pattern using direct builders
        val vectorExpressions = findVectorExpressions(psiFile)
        for (expr in vectorExpressions) {
            val graphic = parseVectorExpression(expr, propertyNameFallback)
            if (graphic != null) {
                return graphic
            }
        }

        error("Failed to parse file ${file.name}")
    } finally {
        Disposer.dispose(disposable)
    }
}

private fun createPsiFile(
    project: Project,
    fileName: String,
    text: String,
): KtFile {
    val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, text)
    return PsiManager.getInstance(project).findFile(virtualFile) as KtFile
}

/**
 * Find property-based ImageVector definitions like:
 * val BackingIcons.Outlined.Add: ImageVector get() = _Add ?: ImageVector.Builder(...).apply { ... }.build()
 */
private fun findPropertyVectors(file: KtFile): List<ImageVectorGraphic> {
    val results = mutableListOf<ImageVectorGraphic>()

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
): ImageVectorGraphic? {
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

        // Now we should have the ImageVector.Builder(...).apply { ... } part
        return parseVectorBuilderExpression(builderExpression, propertyName, packageName)
    }

    return null
}

/**
 * Parse a vector builder expression (ImageVector.Builder(...).apply { ... })
 */
private fun parseVectorBuilderExpression(
    expression: KtExpression,
    propertyName: String,
    packageName: String?,
): ImageVectorGraphic? {
    val elements = mutableListOf<Element>()
    var id: String? = null
    val foreign = mutableMapOf<String, String>()

    // Handle the ImageVector.Builder(...).apply { ... } pattern
    if (expression is KtDotQualifiedExpression) {
        // Extract the Builder part
        val builderExpr = expression.receiverExpression

        if (builderExpr is KtCallExpression ||
            (builderExpr is KtDotQualifiedExpression && builderExpr.selectorExpression is KtCallExpression)
        ) {
            // Extract the builder arguments (name, dimensions, etc.)
            val callExpr =
                builderExpr as? KtCallExpression
                    ?: (builderExpr as KtDotQualifiedExpression).selectorExpression as KtCallExpression

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

            // Parse the apply block containing path definitions
            val applyExpr = expression.selectorExpression
            if (applyExpr is KtCallExpression && applyExpr.calleeExpression?.text == "apply") {
                val lambdaArg = applyExpr.lambdaArguments.firstOrNull()
                val lambdaExpr = lambdaArg?.getLambdaExpression()
                val bodyExpr = lambdaExpr?.bodyExpression

                if (bodyExpr != null) {
                    // Process statements in the apply block (path calls)
                    for (statement in bodyExpr.statements) {
                        when {
                            // Handle path {} builder pattern
                            statement is KtCallExpression && statement.calleeExpression?.text == "path" -> {
                                val pathElement = parsePathBuilderCall(statement)
                                if (pathElement != null) {
                                    elements.add(pathElement)
                                }
                            }
                            // Handle addPath() method (backward compatibility)
                            statement is KtCallExpression && statement.calleeExpression?.text == "addPath" -> {
                                val pathElement = parseAddPathCall(statement)
                                if (pathElement != null) {
                                    elements.add(pathElement)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // todo: handle ImageVector.Builder(...).path(...) { }.group(...) { }.build()

    return if (elements.isNotEmpty() || id != null) {
        ImageVectorGraphic(elements, id, foreign, propertyName, packageName)
    } else {
        null
    }
}

// Previous methods for backward compatibility
private fun findVectorExpressions(file: KtFile): List<PsiElement> {
    val results = mutableListOf<PsiElement>()

    file.accept(
        object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                // Check if it's a direct ImageVector.Builder call
                val calleeExpr = expression.calleeExpression
                if (calleeExpr is KtNameReferenceExpression &&
                    calleeExpr.getReferencedName() == "Builder" ||
                    calleeExpr is KtDotQualifiedExpression &&
                    calleeExpr.text.contains("ImageVector.Builder")
                ) {
                    results.add(expression)
                }
            }

            override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                super.visitQualifiedExpression(expression)

                // Check for Builder().apply{} pattern
                if (expression is KtDotQualifiedExpression) {
                    val selector = expression.selectorExpression
                    if (selector is KtCallExpression) {
                        val calleeExpr = selector.calleeExpression
                        if (calleeExpr is KtNameReferenceExpression && calleeExpr.getReferencedName() == "apply") {
                            when (val receiver = expression.receiverExpression) {
                                is KtCallExpression -> {
                                    val receiverCallee = receiver.calleeExpression
                                    if (receiverCallee is KtNameReferenceExpression &&
                                        receiverCallee.getReferencedName() == "Builder" ||
                                        receiverCallee is KtDotQualifiedExpression &&
                                        receiverCallee.text.contains("ImageVector.Builder")
                                    ) {
                                        results.add(expression)
                                    }
                                }
                                is KtDotQualifiedExpression -> {
                                    if (receiver.text.contains("ImageVector.Builder")) {
                                        results.add(expression)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )

    return results
}

private fun parseVectorExpression(
    element: PsiElement,
    propertyName: String,
): ImageVectorGraphic? {
    val elements = mutableListOf<Element>()
    var id: String? = null
    val foreign = mutableMapOf<String, String>()

    when (val parent = element.parent.parent) {
        // Handle the legacy apply block pattern
        is KtDotQualifiedExpression -> {
            val receiver =
                (parent.receiverExpression as? KtDotQualifiedExpression)?.selectorExpression
                    ?: parent.receiverExpression

            if (receiver is KtCallExpression) {
                // Extract builder parameters
                receiver.valueArgumentList?.arguments?.forEach { arg ->
                    if (arg is KtValueArgument) {
                        val argumentName = arg.getArgumentName()?.asName?.identifier
                        val expression = arg.getArgumentExpression()

                        when (argumentName) {
                            "name" -> {
                                if (expression is KtStringTemplateExpression) {
                                    id =
                                        expression.entries.joinToString("") {
                                            when (it) {
                                                is KtLiteralStringTemplateEntry -> it.text
                                                is KtSimpleNameStringTemplateEntry -> it.text
                                                else -> ""
                                            }
                                        }
                                }
                            }
                            "defaultWidth", "defaultHeight", "viewportWidth", "viewportHeight" -> {
                                if (expression != null) {
                                    foreign[argumentName] = expression.text.removeSuffix(".dp").removeSuffix("f")
                                }
                            }
                        }
                    }
                }

                // Parse the apply block
                val selector = parent.selectorExpression
                if (selector is KtCallExpression) {
                    val lambdaArg = selector.lambdaArguments.firstOrNull()
                    val lambdaExpr = lambdaArg?.getLambdaExpression()
                    val bodyExpr = lambdaExpr?.bodyExpression

                    if (bodyExpr != null) {
                        // Process all statements in the apply block
                        for (statement in bodyExpr.statements) {
                            // Process both addPath and path builder patterns
                            when {
                                // Handle addPath() method
                                statement is KtCallExpression &&
                                    statement.calleeExpression?.text == "addPath" -> {
                                    val pathElement = parseAddPathCall(statement)
                                    if (pathElement != null) {
                                        elements.add(pathElement)
                                    }
                                }

                                // Handle path {} builder pattern
                                statement is KtCallExpression &&
                                    statement.calleeExpression?.text == "path" -> {
                                    val pathElement = parsePathBuilderCall(statement)
                                    if (pathElement != null) {
                                        elements.add(pathElement)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return ImageVectorGraphic(elements, id, foreign, propertyName, null)
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
            val colorValueArg =
                colorArg.valueArgumentList
                    ?.arguments
                    ?.firstOrNull()
                    ?.getArgumentExpression()
            if (colorValueArg is KtConstantExpression) {
                return colorValueArg.text.toUIntOrNull()?.let(::Color)
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

private fun parseAddPathCall(callExpression: KtCallExpression): Path? {
    var pathData: List<Command> = emptyList()
    var fillColor: Color? = null
    var strokeColor: Color? = null
    var strokeWidth: Float? = null
    var fillAlpha: Float? = null
    var strokeAlpha: Float? = null

    // Extract named arguments from addPath call
    callExpression.valueArgumentList?.arguments?.forEach { arg ->
        if (arg is KtValueArgument) {
            val argumentName = arg.getArgumentName()?.asName?.identifier
            val expression = arg.getArgumentExpression()

            when (argumentName) {
                "pathData" -> {
                    pathData = parsePathDataExpression(expression)
                }
                "fill" -> {
                    fillColor = parseColorArgument(expression)
                }
                "stroke" -> {
                    strokeColor = parseColorArgument(expression)
                }
                "strokeLineWidth" -> {
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
        commands = pathData,
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

private fun parsePathDataExpression(expression: KtExpression?): List<Command> {
    if (expression == null) {
        return emptyList()
    }

    // PathData with string literal argument
    if (expression is KtCallExpression && expression.calleeExpression?.text == "PathData") {
        val argList = expression.valueArgumentList
        if (argList != null && argList.arguments.size == 1) {
            val argExpr = argList.arguments[0].getArgumentExpression()

            // Case 1a: PathData with a single string
            if (argExpr is KtStringTemplateExpression) {
                return CommandString(argExpr.entries.joinToString("") { it.text }).toCommandList()
            }

            // Case 1b: PathData with a collection of strings
            if (argExpr is KtCallExpression && argExpr.calleeExpression?.text == "listOf") {
                return argExpr.valueArgumentList
                    ?.arguments
                    ?.mapNotNull { arg ->
                        val stringExpr = arg.getArgumentExpression() as? KtStringTemplateExpression
                        stringExpr?.entries?.joinToString("") { it.text }
                    }?.joinToString("")
                    ?.let { CommandString(it).toCommandList() } ?: emptyList()
            }
        }
    }

    return emptyList()
}
