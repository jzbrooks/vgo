package com.jzbrooks.vgo.util

import org.jetbrains.kotlin.cli.jvm.compiler.IdeaStandaloneExecutionSetup
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreApplicationEnvironmentMode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile

/**
 * Parses Kotlin source into a syntax tree.
 *
 * Only the parser is set up — no classpath, no resolution — because
 * ImageVector graphics are read straight from the syntax tree.
 *
 * [disposable] owns the parser environment. Disposing it invalidates
 * the returned file.
 */
internal fun parseKotlinSource(
    disposable: Disposable,
    fileName: String,
    text: String,
): KtFile {
    IdeaStandaloneExecutionSetup.doSetup()

    val applicationEnvironment =
        KotlinCoreApplicationEnvironment
            .create(disposable, KotlinCoreApplicationEnvironmentMode.Production)

    applicationEnvironment.registerFileType(KotlinFileType.INSTANCE, KotlinFileType.EXTENSION)
    applicationEnvironment.registerParserDefinition(KotlinParserDefinition())

    val projectEnvironment = KotlinCoreProjectEnvironment(disposable, applicationEnvironment)

    val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, text)
    return PsiManager.getInstance(projectEnvironment.project).findFile(virtualFile) as KtFile
}
