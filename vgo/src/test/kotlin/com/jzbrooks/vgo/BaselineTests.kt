package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.hasText
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import org.junit.jupiter.api.MediaType
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.asSequence

class BaselineTests {
    @ParameterizedTest
    @MethodSource("provideUnoptimizedAssets")
    fun testOptimizationFinishes(
        unoptimizedAsset: Path,
        testReporter: TestReporter,
    ) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/test-results/${inputFileName}_testOptimizationFinishes.${inputFile.extension}"

        val options =
            Vgo.Options(
                indent = 2,
                input = listOf(unoptimizedAsset.toString()),
                output = listOf(outputFilePath),
            )

        val exitCode = Vgo(options).run()

        assertThat(exitCode).isEqualTo(0)

        testReporter.publishFile(Path.of(outputFilePath), mediaTypeFor(inputFile.extension))
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAndOptimizedAssets")
    fun testOptimizedAssetIsEquivalentToBaseline(
        unoptimizedAsset: Path,
        baselineAsset: Path,
        testReporter: TestReporter,
    ) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/test-results/${inputFileName}_testOptimizedAssetIsEquivalentToBaseline.${inputFile.extension}"
        val options =
            Vgo.Options(
                indent = 2,
                input = listOf(unoptimizedAsset.toString()),
                output = listOf(outputFilePath),
            )

        Vgo(options).run()

        testReporter.publishFile(Path.of(outputFilePath), mediaTypeFor(inputFile.extension))

        val content = File(outputFilePath)
        val baselineContent = baselineAsset.toFile()
        assertThat(content, "optimized asset").hasText(baselineContent.readText())
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAndOptimizedAssets")
    fun testOptimizedAssetIsNotLargerThanBaseline(
        unoptimizedAsset: Path,
        baselineAsset: Path,
        testReporter: TestReporter,
    ) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/test-results/${inputFileName}_testOptimizedAssetIsNotLargerThanBaseline.${inputFile.extension}"
        val options =
            Vgo.Options(
                indent = 2,
                input = listOf(unoptimizedAsset.toString()),
                output = listOf(outputFilePath),
            )

        Vgo(options).run()

        testReporter.publishFile(Path.of(outputFilePath), mediaTypeFor(inputFile.extension))

        val optimizedAssetSize = File(outputFilePath).length()
        val baselineAssetSize = baselineAsset.toFile().length()

        assertThat(optimizedAssetSize, "optimized size").isLessThanOrEqualTo(baselineAssetSize)
    }

    @ParameterizedTest
    @MethodSource("provideImageVectorAssets")
    fun testImageVectorIsEquivalentToBaseline(
        unoptimizedAsset: Path,
        baselineAsset: Path,
        testReporter: TestReporter,
    ) {
        val inputFile = unoptimizedAsset.toFile()
        val outputFilePath =
            "build/test-results/${inputFile.nameWithoutExtension}_testImageVectorIsEquivalentToBaseline.kt"
        val options =
            Vgo.Options(
                indent = 2,
                input = listOf(unoptimizedAsset.toString()),
                output = listOf(outputFilePath),
            )

        val exitCode = Vgo(options).run()

        assertThat(exitCode).isEqualTo(0)
        testReporter.publishFile(Path.of(outputFilePath), MediaType.TEXT_PLAIN)
        assertThat(File(outputFilePath), "optimized ImageVector").hasText(baselineAsset.toFile().readText())
    }

    @ParameterizedTest
    @MethodSource("provideUnoptimizedAssets")
    fun testOptimizedAssetIsNotLargerThanOriginal(
        unoptimizedAsset: Path,
        testReporter: TestReporter,
    ) {
        val inputFile = unoptimizedAsset.toFile()
        val inputFileName = inputFile.name.substring(0, inputFile.name.lastIndexOf('.'))
        val outputFilePath = "build/test-results/${inputFileName}_testOptimizedAssetIsNotLargerThanOriginal.${inputFile.extension}"
        val options =
            Vgo.Options(
                indent = 2,
                input = listOf(unoptimizedAsset.toString()),
                output = listOf(outputFilePath),
            )

        Vgo(options).run()

        testReporter.publishFile(Path.of(outputFilePath), mediaTypeFor(inputFile.extension))

        val optimizedAssetSize = File(outputFilePath).length()
        val unoptimizedAssetSize = unoptimizedAsset.toFile().length()

        assertThat(optimizedAssetSize, "optimized asset").isLessThanOrEqualTo(unoptimizedAssetSize)
    }

    companion object {
        private fun mediaTypeFor(extension: String): MediaType =
            when (extension) {
                "svg" -> MediaType.parse("image/svg+xml")
                "xml" -> MediaType.create("application", "xml")
                else -> MediaType.TEXT_PLAIN
            }

        // Loads the files based on the convention that optimized files
        // live in src/test/resources/baseline and are suffixed with _optimized
        private val assets: List<Pair<Path, Path>> =
            try {
                Files
                    .list(Paths.get("src/test/resources"))
                    .asSequence()
                    .filterNot { Files.isDirectory(it) }
                    .sortedBy { it.fileName.toString() }
                    .map { unoptimizedFile ->
                        val (fileName, fileExtension) = unoptimizedFile.fileName.toString().split(".")
                        val optimizedDirectory = unoptimizedFile.parent.resolve("baseline")
                        unoptimizedFile to optimizedDirectory.resolve("${fileName}_optimized.$fileExtension")
                    }.toList()
            } catch (e: Throwable) {
                System.err.println(e)
                throw e
            }

        private val imageVectorAssets: List<Pair<Path, Path>> =
            Files
                .walk(Paths.get("src/test/resources/imagevector"), 2)
                .asSequence()
                .filterNot { Files.isDirectory(it) }
                .sortedBy { it.fileName.toString() }
                .map { unoptimizedFile ->
                    val completeStem = unoptimizedFile.fileName.nameWithoutExtension
                    unoptimizedFile to Paths.get("src/test/resources/baseline", "${completeStem}_optimized.kt")
                }.toList()

        @JvmStatic
        fun provideUnoptimizedAssets(): List<Arguments> =
            assets.map {
                Arguments.of(it.first)
            }

        @JvmStatic
        fun provideUnoptimizedAndOptimizedAssets(): List<Arguments> =
            assets.map {
                Arguments.of(it.first, it.second)
            }

        @JvmStatic
        fun provideImageVectorAssets(): List<Arguments> =
            imageVectorAssets.map {
                Arguments.of(it.first, it.second)
            }
    }
}
