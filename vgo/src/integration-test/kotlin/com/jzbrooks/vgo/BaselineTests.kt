package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path

class BaselineTests {

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAssets")
    fun testOptimizationFinishes(unoptimizedAsset: Path) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/integrationTest/$inputFileName/testOptimizationFinishes.${inputFile.extension}"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAndOptimizedAssets")
    fun testOptimizedAssetIsEquivalentToBaseline(unoptimizedAsset: Path, baselineAsset: Path) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/integrationTest/$inputFileName/testOptimizedAssetIsEquivalentToBaseline.${inputFile.extension}"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        Application().run(arguments)

        val content = File(outputFilePath).readText()
        val baselineContent = baselineAsset.toFile().readText()
        assertThat(content).isEqualTo(baselineContent)
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAndOptimizedAssets")
    fun testOptimizedAssetIsNotLargerThanBaseline(unoptimizedAsset: Path, baselineAsset: Path) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/integrationTest/$inputFileName/testOptimizedAssetIsNotLargerThanBaseline.${inputFile.extension}"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        Application().run(arguments)

        val optimizedAssetSize = File(outputFilePath).length()
        val baselineAssetSize = baselineAsset.toFile().length()

        assertThat(optimizedAssetSize).isLessThanOrEqualTo(baselineAssetSize)
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAssets")
    fun testOptimizedAssetIsNotLargerThanOriginal(unoptimizedAsset: Path) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/integrationTest/$inputFileName/testOptimizedAssetIsNotLargerThanOriginal.${inputFile.extension}"
        val arguments = arrayOf(unoptimizedAsset.toString(), "-o", outputFilePath)

        Application().run(arguments)

        val optimizedAssetSize = File(outputFilePath).length()
        val unoptimizedAssetSize = unoptimizedAsset.toFile().length()

        assertThat(optimizedAssetSize).isLessThanOrEqualTo(unoptimizedAssetSize)
    }

    companion object {
        private val assets = listOf(
                Path.of("src/integration-test/resources/avocado_example.xml") to Path.of("src/integration-test/resources/baseline/avocado_example_optimized.xml"),
                Path.of("src/integration-test/resources/charging_battery.xml") to Path.of("src/integration-test/resources/baseline/charging_battery_optimized.xml"),
                Path.of("src/integration-test/resources/simple_heart.xml") to Path.of("src/integration-test/resources/baseline/simple_heart_optimized.xml"),
                Path.of("src/integration-test/resources/visibility_strike.xml") to Path.of("src/integration-test/resources/baseline/visibility_strike_optimized.xml"),
                Path.of("src/integration-test/resources/dribbble_ball_mark.xml") to Path.of("src/integration-test/resources/baseline/dribbble_ball_mark_optimized.xml"),
                Path.of("src/integration-test/resources/nasa.xml") to Path.of("src/integration-test/resources/baseline/nasa_optimized.xml"),
                Path.of("src/integration-test/resources/simple_heart.svg") to Path.of("src/integration-test/resources/baseline/simple_heart_optimized.svg"),
                Path.of("src/integration-test/resources/guacamole.svg") to Path.of("src/integration-test/resources/baseline/guacamole_optimized.svg"),
                Path.of("src/integration-test/resources/dribbble_ball_mark.svg") to Path.of("src/integration-test/resources/baseline/dribbble_ball_mark_optimized.svg"),
                Path.of("src/integration-test/resources/nasa.svg") to Path.of("src/integration-test/resources/baseline/nasa_optimized.svg")
        )

        @JvmStatic
        private fun provideUnoptimizedAssets(): List<Arguments> {
            return assets.map {
                Arguments.of(it.first)
            }
        }

        @JvmStatic
        private fun provideUnoptimizedAndOptimizedAssets(): List<Arguments> {
            return assets.map {
                Arguments.of(it.first, it.second)
            }
        }
    }
}