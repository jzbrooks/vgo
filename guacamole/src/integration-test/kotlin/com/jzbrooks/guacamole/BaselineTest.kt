package com.jzbrooks.guacamole

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import org.junit.Test
import java.io.File
import java.nio.file.Path

abstract class BaselineTest(private val unoptimizedAsset: Path, private val baselineAsset: Path) {
    @Test
    fun testOptimizationFinishes() {
        val outputFilePath = "build/integrationTest/${this::class.java.simpleName}/testOptimizationFinishes.xml"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun testOptimizedAssetIsEquivalentToBaseline() {
        val outputFilePath = "build/integrationTest/${this::class.java.simpleName}/testOptimizationIsCompact.xml"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        App().run(arguments)

        val content = File(outputFilePath).readText()
        val baselineContent = baselineAsset.toFile().readText()
        assertThat(content).isEqualTo(baselineContent)
    }

    @Test
    fun testOptimizedAssetIsNotLargerThanBaseline() {
        val outputFilePath = "build/integrationTest/${this::class.java.simpleName}/testOptimizedAssetIsNotLargerThanBaseline.xml"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        App().run(arguments)

        val optimizedAssetSize = File(outputFilePath).length()
        val baselineAssetSize = baselineAsset.toFile().length()

        assertThat(optimizedAssetSize).isLessThanOrEqualTo(baselineAssetSize)
    }
}