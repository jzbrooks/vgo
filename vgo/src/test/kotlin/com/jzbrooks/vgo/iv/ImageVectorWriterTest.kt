package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.util.element.createPath
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
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ImageVectorWriterTest {

    @Test
    fun testExtraChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ImageVectorWriter().write(graphic, memoryStream)

            assertKotlinStructure(memoryStream) {
                var actual: KtProperty? = null
                it.accept(
                    object : KtTreeVisitorVoid() {
                        override fun visitProperty(property: KtProperty) {
                            super.visitProperty(property)
                            if (property.visibilityModifier()?.text == "public")
                                actual = property
                        }
                    }
                )

                assertThat(actual).isNotNull()
                    .transform("typeReference") { it.typeReference }
                    .isNotNull()
                    .transform("text") { it.text }
                    .isEqualTo("ImageVector")
            }
        }
    }

    private fun assertKotlinStructure(file: ByteArrayOutputStream, assertion: (KtFile) -> Unit) {
        val text =
            ByteArrayInputStream(file.toByteArray())
            .bufferedReader()
            .use { it.readText() }

        val disposable = Disposer.newDisposable()

        return try {
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
            val psiFile = PsiManager.getInstance(environment.project).findFile(virtualFile) as KtFile

            assertion(psiFile)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    companion object {
        val graphic =
            ImageVector(
                listOf(
                    createPath(
                        CommandString("M 2 4.27 L 3.27 3 L 3.27 3 L 2 4.27 Z").toCommandList(),
                        "strike_thru_path",
                    ),
                    createPath(
                        CommandString(
                            "M 12 4.5 C 7 4.5 2.73 7.61 1 12 C 2.73 16.39 7 19.5 12 19.5 " +
                                "C 17 19.5 21.27 16.39 23 12 C 21.27 7.61 17 4.5 12 4.5 L 12 4.5 Z " +
                                "M 12 17 C 9.24 17 7 14.76 7 12 C 7 9.24 9.24 7 12 7 C 14.76 7 17 9.24 17 12 " +
                                "C 17 14.76 14.76 17 12 17 L 12 17 Z M 12 9 C 10.34 9 9 10.34 9 12 " +
                                "C 9 13.66 10.34 15 12 15 C 13.66 15 15 13.66 15 12 C 15 10.34 13.66 9 12 9 " +
                                "L 12 9 Z",
                        ).toCommandList(),
                    ),
                ),
                null,
                mutableMapOf(
                    "defaultWidth" to "24.dp",
                    "defaultHeight" to "24.dp",
                    "viewportWidth" to "24.dp",
                    "viewportHeight" to "24.dp",
                ),
                "image",
                null
            )
    }
}