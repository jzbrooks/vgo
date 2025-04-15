package com.jzbrooks.vgo.iv

import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.InputStream

fun parseKotlinFile(
    disposable: Disposable,
    input: InputStream,
): KtFile {
    val text = input.reader().readText()
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

    val virtualFile = LightVirtualFile("test.kt", KotlinFileType.INSTANCE, text)
    return PsiManager.getInstance(environment.project).findFile(virtualFile) as KtFile
}
