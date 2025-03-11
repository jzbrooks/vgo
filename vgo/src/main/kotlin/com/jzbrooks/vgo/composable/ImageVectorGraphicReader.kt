package com.jzbrooks.vgo.composable

import com.jzbrooks.vgo.core.Color
import com.jzbrooks.vgo.core.graphic.Element
import com.jzbrooks.vgo.core.graphic.Path
import com.jzbrooks.vgo.core.graphic.command.Command
import com.jzbrooks.vgo.core.graphic.command.CommandString
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtValueArgument
import java.io.File
import java.util.UUID

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

        val project = environment.project
        val file = createPsiFile(project, file.name, text)

        // Find all potential vector building expressions
        val vectorExpressions = findVectorExpressions(file)

        // Parse the first valid expression found (if any)
        for (expr in vectorExpressions) {
            val graphic = parseVectorExpression(expr, file.name)
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

private fun findVectorExpressions(file: KtFile): List<PsiElement> {
    val results = mutableListOf<PsiElement>()

    file.accept(object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
            super.visitCallExpression(expression)

            // Check if it's a direct ImageVector.Builder call
            val calleeExpr = expression.calleeExpression
            if (calleeExpr is KtNameReferenceExpression && calleeExpr.getReferencedName() == "Builder" ||
                calleeExpr is KtDotQualifiedExpression && calleeExpr.text.contains("ImageVector.Builder")) {
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
                        val receiver = expression.receiverExpression
                        when (receiver) {
                            is KtCallExpression -> {
                                val receiverCallee = receiver.calleeExpression
                                if (receiverCallee is KtNameReferenceExpression && receiverCallee.getReferencedName() == "Builder" ||
                                    receiverCallee is KtDotQualifiedExpression && receiverCallee.text.contains("ImageVector.Builder")) {
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
    })

    return results
}

private fun parseVectorExpression(element: PsiElement, fileName: String): ImageVectorGraphic? {
    val elements = mutableListOf<Element>()
    var id: String? = null
    val foreign = mutableMapOf<String, String>()

    val parent = element.parent.parent
    when (parent) {
        // todo: handle when the image builder is stored in variable
        is KtDotQualifiedExpression -> {
            val receiver = (parent.receiverExpression as KtDotQualifiedExpression).selectorExpression
            if (receiver is KtCallExpression) {
                receiver.valueArgumentList?.arguments?.forEach { arg ->
                    if (arg is KtValueArgument) {
                        val argumentName = arg.getArgumentName()?.asName?.identifier
                        val expression = arg.getArgumentExpression()

                        when (argumentName) {
                            "name" -> {
                                if (expression is KtStringTemplateExpression) {
                                    id = expression.entries.joinToString("") {
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
                                    foreign[argumentName] = expression.text
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
                            if (statement is KtCallExpression &&
                                statement.calleeExpression?.text == "addPath") {
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

    // If no ID was found, generate a random one
    if (id == null) {
        id = UUID.randomUUID().toString()
    }

    return ImageVectorGraphic(elements, id, fileName, null).apply {
        this.foreign.putAll(foreign)
    }
}

private fun parseAddPathCall(callExpression: KtCallExpression): Path? {
    var pathData: List<Command> = emptyList()
    var fillColor: Int? = null
    var strokeColor: Int? = null
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
                    fillColor = (expression as? KtConstantExpression)?.text?.toIntOrNull()
                }
                "stroke" -> {
                    strokeColor = (expression as? KtConstantExpression)?.text?.toIntOrNull()
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

    return Path(
        id = null, // todo
        commands = pathData,
        fill = Color(0x00000000u), // todo
        stroke = Color(0xFFFFFFFFu), // todo
        strokeWidth = 1f, // todo
        fillRule = Path.FillRule.EVEN_ODD, // todo
        strokeLineCap = Path.LineCap.BUTT, // todo
        strokeLineJoin = Path.LineJoin.MITER, // todo
        strokeMiterLimit = 4f, // todo?
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