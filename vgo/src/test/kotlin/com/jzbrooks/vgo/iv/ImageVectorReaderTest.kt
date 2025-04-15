package com.jzbrooks.vgo.iv

import assertk.assertThat
import assertk.assertions.isNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class ImageVectorReaderTest {
    private lateinit var file: File

    @BeforeEach
    fun setup() {
        val uri = checkNotNull(javaClass.getResource("/star.kt"))
        file = File(uri.toURI())
    }

    @Test
    fun `graphic is parsed`() {
        val graphic = parse(file)

        assertThat(graphic).isNotNull()
    }

}