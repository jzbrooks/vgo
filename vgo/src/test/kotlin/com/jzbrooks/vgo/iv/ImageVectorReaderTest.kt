package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.isNotNull
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
}
