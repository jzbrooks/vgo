package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AvocadoExampleTests {
    private val avocadoExampleRelativePath = "src/integrationTest/resources/avacado_example.xml"
    @Test
    fun testOptimizationFinishes() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/test.xml")
        val process = ProcessBuilder("java", "-jar", "build/libs/guacamole.jar", *arguments)
                .start()

        assertThat(process.waitFor()).isEqualTo(0)
    }
}