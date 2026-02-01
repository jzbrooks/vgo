package com.jzbrooks.vgo.util

import com.android.ide.common.vectordrawable.Svg2Vector
import com.jzbrooks.vgo.core.graphic.Graphic
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import com.jzbrooks.vgo.iv.parse as composableParse
import com.jzbrooks.vgo.svg.parse as svgParse
import com.jzbrooks.vgo.vd.parse as vdParse

fun parse(file: File): Graphic? = parse(file, null)

@OptIn(ExperimentalVgoApi::class)
internal fun parse(
    file: File,
    format: String? = null,
): Graphic? {
    if (file.length() == 0L) return null

    return file.inputStream().use { inputStream ->
        if (file.extension == "kt") {
            val text = file.reader().readText()
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
                val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, text)
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as KtFile

                composableParse(psiFile)
            } finally {
                Disposer.dispose(disposable)
            }
        } else {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val document = documentBuilderFactory.newDocumentBuilder().parse(inputStream)
            document.documentElement.normalize()

            val graphic =
                when {
                    document.documentElement.nodeName == "svg" || file.extension == "svg" -> {
                        if (format == "vd") {
                            ByteArrayOutputStream().use { pipeOrigin ->
                                val errors = Svg2Vector.parseSvgToXml(file.toPath(), pipeOrigin)
                                if (errors != "") {
                                    System.err.println(
                                        """
                                        Skipping ${file.path}
                                            $errors
                                        """.trimIndent(),
                                    )
                                    null
                                } else {
                                    val pipeTerminal = ByteArrayInputStream(pipeOrigin.toByteArray())
                                    val convertedDocument =
                                        documentBuilderFactory.newDocumentBuilder().parse(pipeTerminal)
                                    convertedDocument.documentElement.normalize()

                                    vdParse(convertedDocument.documentElement)
                                }
                            }
                        } else {
                            svgParse(document.documentElement)
                        }
                    }

                    document.documentElement.nodeName == "vector" && file.extension == "xml" -> {
                        vdParse(document.documentElement)
                    }

                    else -> {
                        null
                    }
                }

            graphic
        }
    }
}
