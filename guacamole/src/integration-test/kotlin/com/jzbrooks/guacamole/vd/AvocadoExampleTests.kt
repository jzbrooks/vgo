package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jzbrooks.guacamole.App
import org.junit.Test

class AvocadoExampleTests {
    private val avocadoExampleRelativePath = "src/integration-test/resources/avocado_example.xml"

    @Test
    fun testOptimizationFinishes() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/test.xml")

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }
}