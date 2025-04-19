package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jzbrooks.vgo.core.graphic.command.CommandString
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.util.element.createPath
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalVgoApi::class)
class ImageVectorWriterTest {
    private lateinit var disposable: Disposable

    @BeforeEach
    fun setup() {
        disposable = Disposer.newDisposable()
    }

    @AfterEach
    fun teardown() {
        disposable.dispose()
    }

    @Test
    fun testExtraChildrenWritten() {
        ByteArrayOutputStream().use { memoryStream ->
            ImageVectorWriter().write(graphic, memoryStream)

            val inputStream = ByteArrayInputStream(memoryStream.toByteArray())
            val psiFile = parseKotlinFile(disposable, inputStream)
            var actual: KtProperty? = null
            psiFile.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        if (property.visibilityModifier()?.text == "public") {
                            actual = property
                        }
                    }
                },
            )

            assertThat(actual)
                .isNotNull()
                .transform("typeReference") { it.typeReference }
                .isNotNull()
                .transform("text") { it.text }
                .isEqualTo("ImageVector")
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
                id = null,
                mutableMapOf(),
                24f,
                24f,
                24f,
                24f,
            )
    }
}
