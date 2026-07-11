package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.hasText
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import com.jzbrooks.vgo.core.util.ExperimentalVgoApi
import com.jzbrooks.vgo.iv.ImageVector
import com.jzbrooks.vgo.iv.ImageVectorWriter
import com.jzbrooks.vgo.iv.parseKotlinFile
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.MediaType
import org.junit.jupiter.api.TestReporter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.asSequence
import com.jzbrooks.vgo.iv.parse as ivParse

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

    // Alternate builder forms of the baseline images live in
    // src/test/resources/imagevector, named <baseline name>.<form>.kt.
    // Each contains the same image data as its baseline, so both must
    // parse to the same image. The full pipeline can't be compared
    // byte-for-byte here: Vgo only writes output that shrinks the file,
    // and re-optimizing a baseline's rounded coordinates can legitimately
    // find conversions the original unrounded input did not.
    @OptIn(ExperimentalVgoApi::class)
    @ParameterizedTest
    @MethodSource("provideAlternateFormAssets")
    fun testAlternateBuilderFormIsEquivalentToBaseline(
        formAsset: Path,
        baselineAsset: Path,
    ) {
        val disposable = Disposer.newDisposable()
        try {
            val formGraphic = ivParse(parseKotlinFile(disposable, Files.newInputStream(formAsset)))
            val baselineGraphic = ivParse(parseKotlinFile(disposable, Files.newInputStream(baselineAsset)))

            assertThat(writeToString(formGraphic), "parsed form asset").isEqualTo(writeToString(baselineGraphic))
        } finally {
            disposable.dispose()
        }
    }

    @OptIn(ExperimentalVgoApi::class)
    private fun writeToString(graphic: ImageVector): String {
        val stream = ByteArrayOutputStream()
        ImageVectorWriter("test").write(graphic, stream)
        return stream.toString()
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
                    .map { unoptimizedFile ->
                        val (fileName, fileExtension) = unoptimizedFile.fileName.toString().split(".")
                        val optimizedDirectory = unoptimizedFile.parent.resolve("baseline")
                        unoptimizedFile to optimizedDirectory.resolve("${fileName}_optimized.$fileExtension")
                    }.toList()
            } catch (e: Throwable) {
                System.err.println(e)
                throw e
            }

        // Alternate builder forms are named <baseline name>.<form>.kt
        private val alternateFormAssets: List<Pair<Path, Path>> =
            Files
                .list(Paths.get("src/test/resources/imagevector"))
                .asSequence()
                .sortedBy { it.fileName.toString() }
                .map { formFile ->
                    val baselineName = formFile.fileName.toString().substringBefore('.')
                    formFile to Paths.get("src/test/resources/baseline", "${baselineName}_optimized.kt")
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
        fun provideAlternateFormAssets(): List<Arguments> =
            alternateFormAssets.map {
                Arguments.of(it.first, it.second)
            }
    }
}
