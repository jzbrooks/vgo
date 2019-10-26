package com.jzbrooks.guacamole.vd

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import com.jzbrooks.guacamole.App
import org.junit.Test
import java.io.File

class AvocadoExampleTests {
    private val avocadoExamplePath = "src/integration-test/resources/avocado_example.xml"
    private val baselineAvocadoExamplePath = "src/integration-test/resources/baseline/avocado_example_optimized.xml"
    @Test
    fun testOptimizationFinishes() {
        val outputFilePath = "build/integrationTest/testOptimizationFinishes.xml"
        val arguments = arrayOf(avocadoExamplePath, "-o", outputFilePath)

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun testOptimizedAssetIsEquivalentToBaseline() {
        val outputFilePath = "build/integrationTest/testOptimizationIsCompact.xml"
        val arguments = arrayOf(avocadoExamplePath, "-o", outputFilePath)

        App().run(arguments)

        val content = File(outputFilePath).readText()
        val baselineContent = File(baselineAvocadoExamplePath).readText()
        assertThat(content).isEqualTo(baselineContent)
    }

    @Test
    fun testOptimizedAssetIsNotLargerThanBaseline() {
        val outputFilePath = "build/integrationTest/testOptimizedAssetIsNotLargerThanBaseline.xml"
        val arguments = arrayOf(avocadoExamplePath, "-o", outputFilePath)

        App().run(arguments)

        val optimizedAssetSize = File(outputFilePath).length()
        val baselineAssetSize = File(baselineAvocadoExamplePath).length()

        assertThat(optimizedAssetSize).isLessThanOrEqualTo(baselineAssetSize)
    }
}