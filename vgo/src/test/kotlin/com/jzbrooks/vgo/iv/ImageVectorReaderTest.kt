package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import assertk.assertions.single
import com.jzbrooks.vgo.core.graphic.Path
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.InputStream

class ImageVectorReaderTest {
    private lateinit var inputStream: InputStream
    private lateinit var disposable: Disposable

    @BeforeEach
    fun setup() {
        inputStream = javaClass.getResourceAsStream("/star.kt")!!
        disposable = Disposer.newDisposable()
    }

    @AfterEach
    fun teardown() {
        disposable.dispose()
        inputStream.close()
    }

    @Test
    fun `graphic is parsed`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic).isNotNull()
    }

    @Test
    fun `path elements are parsed`() {
        val psiFile = parseKotlinFile(disposable, inputStream)
        val graphic = parse(psiFile)

        assertThat(graphic::elements)
            .single()
            .isInstanceOf<Path>()
            .prop(Path::commands)
            .hasSize(11)
    }
}
