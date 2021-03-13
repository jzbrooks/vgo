package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths

class CommandLineInterfaceTests {
    private val avocadoExampleRelativePath = Paths.get("src/test/resources/avocado_example.xml").toString()
    private val heartExampleRelativePath = Paths.get("src/test/resources/simple_heart.xml").toString()
    private lateinit var systemOutput: ByteArrayOutputStream

    @BeforeEach
    fun redirect() {
        systemOutput = ByteArrayOutputStream()
        System.setOut(PrintStream(systemOutput))
    }

    @AfterEach
    fun cleanup() {
        systemOutput.close()
    }

    @Test
    fun testLongVersionFlag() {
        val arguments = arrayOf("--version")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).matches(Regex("\\d+.\\d+.\\d+\r?\n"))
    }

    @Test
    fun testShortVersionFlag() {
        val arguments = arrayOf("-v")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).matches(Regex("\\d+.\\d+.\\d+\r?\n"))
    }

    @Test
    fun testLongHelpFlag() {
        val arguments = arrayOf("--help")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("vgo")
        assertThat(output).contains("Options")
    }

    @Test
    fun testShortHelpFlag() {
        val arguments = arrayOf("-h")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("vgo")
        assertThat(output).contains("Options")
    }

    @Test
    fun testLongStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "--stats")
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun `unmodified files are omitted from statistics`() {
        val input = "src/test/resources/baseline/simple_heart_optimized.xml"
        val arguments = arrayOf(
                input,
                "-o",
                "build/integrationTest/unmodified-stats-omitted.xml",
                "--stats"
        )
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val report = systemOutput.toString()
        assertThat(report).doesNotContain(input)
    }

    @Test
    fun `directory inputs include a filename with statistics`() {
        val arguments = arrayOf(
                "src/test/resources/in-place-modify",
                "-o",
                "build/integrationTest/multi-stats-test-directory",
                "--stats"
        )
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val report = systemOutput.toString()
        assertThat(report).contains(Paths.get("src/test/resources/in-place-modify/avocado_example.xml").toString())
    }

    @Test
    fun `multiple file inputs include a filename with statistics`() {
        val arguments = arrayOf(
                avocadoExampleRelativePath,
                "-o",
                "build/integrationTest/multi-stats-test-one.xml",
                heartExampleRelativePath,
                "-o",
                "build/integrationTest/multi-stats-test-two.xml",
                "--stats"
        )
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val report = systemOutput.toString()
        assertThat(report).contains(avocadoExampleRelativePath)
        assertThat(report).contains(heartExampleRelativePath)
    }

    @Test
    fun testShortStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "-s")
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testOverwritingInputFileReportsNonZeroSizeChange() {
        val overwritePath = Paths.get("build/integrationTest/stat-overwrite-test.xml")
        File(avocadoExampleRelativePath).copyTo(overwritePath.toFile(), overwrite = true)

        val arguments = arrayOf(overwritePath.toString(), "-s")
        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
        assertThat(systemOutput.toString()).doesNotContain("Percent saved: 0.0")
    }

    @Test
    fun testIndentOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/indent-test.xml", "--indent", "4")
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/indent-test.xml").readText()
        assertThat(output).contains("    <path")
    }

    @Test
    fun testFormatOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/format-test.svg", "--format", "svg")
        val exitCode = Application().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/format-test.svg").readText()
        assertThat(output).startsWith("<svg")
    }

    companion object {
        @BeforeAll
        fun createTempDir() {
            File("build/integrationTest").apply {
                mkdir()
                deleteOnExit()
            }
        }
    }
}