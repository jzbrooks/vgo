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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
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

        val builderCall = findImageVectorBuilderCall(file)
        if (builderCall != null) {
            return parseImageVectorBuilder(builderCall)
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

private fun findImageVectorBuilderCall(file: KtFile): KtCallExpression? {
    var builderCall: KtCallExpression? = null

    file.accept(
        object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)

                val calleeExpr = expression.calleeExpression
                if (calleeExpr is KtNameReferenceExpression &&
                    calleeExpr.getReferencedName() == "Builder" ||
                    calleeExpr is KtDotQualifiedExpression &&
                    calleeExpr.text == "ImageVector.Builder"
                ) {
                    builderCall = expression
                }
            }
        },
    )

    return builderCall
}

private fun parseImageVectorBuilder(callExpression: KtCallExpression): ImageVectorGraphic {
    // Extract parameters and create ImageVectorGraphic
    val elements = mutableListOf<Element>()
    var id: String? = null
    val foreign = mutableMapOf<String, String>()

    // Extract named arguments from the builder call
    callExpression.valueArgumentList?.arguments?.forEach { arg ->
        if (arg is KtValueArgument) {
            val argumentName = arg.getArgumentName()?.asName?.identifier
            val expression = arg.getArgumentExpression()

            when (argumentName) {
                "name" -> {
                    if (expression is KtStringTemplateExpression) {
                        id = expression.entries.joinToString("") { it.text }
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

    // If no ID was found, generate a random one
    if (id == null) {
        id = UUID.randomUUID().toString()
    }

    // Process lambda body to find addPath calls
    callExpression.lambdaArguments.forEach { lambdaArg ->
        lambdaArg.getLambdaExpression()?.bodyExpression?.statements?.forEach { statement ->
            if (statement is KtCallExpression && statement.calleeExpression?.text == "addPath") {
                val pathElement = parseAddPathCall(statement)
                if (pathElement != null) {
                    elements.add(pathElement)
                }
            }
        }
    }

    return ImageVectorGraphic(elements, id).apply {
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

// fun findPathDataReferences(element: PsiElement): List<KtCallExpression> {
//    val result = mutableListOf<KtCallExpression>()
//
//    element.accept(
//        object : KtTreeVisitorVoid() {
//            override fun visitCallExpression(expression: KtCallExpression) {
//                super.visitCallExpression(expression)
//
//                val calleeExpr = expression.calleeExpression
//                if (calleeExpr is KtNameReferenceExpression && calleeExpr.getReferencedName() == "PathData") {
//                    result.add(expression)
//                }
//            }
//        },
//    )
//
//    return result
// }
