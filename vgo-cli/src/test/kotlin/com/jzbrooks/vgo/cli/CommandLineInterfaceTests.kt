package com.jzbrooks.vgo.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.hasLineCount
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import assertk.assertions.startsWith
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
    private lateinit var systemError: ByteArrayOutputStream
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream

    @BeforeEach
    fun redirect() {
        originalOut = System.out
        originalErr = System.err
        systemOutput = ByteArrayOutputStream()
        systemError = ByteArrayOutputStream()
        System.setOut(PrintStream(systemOutput))
        System.setErr(PrintStream(systemError))
    }

    @AfterEach
    fun cleanup() {
        System.setOut(originalOut)
        System.setErr(originalErr)
        systemOutput.close()
        systemError.close()
    }

    @Test
    fun testLongVersionFlag() {
        val arguments = arrayOf("--version")

        val exitCode = CommandLineInterface().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).matches(Regex("\\d+.\\d+.\\d+\r?\n"))
    }

    @Test
    fun testShortVersionFlag() {
        val arguments = arrayOf("-v")

        val exitCode = CommandLineInterface().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).matches(Regex("\\d+.\\d+.\\d+\r?\n"))
    }

    @Test
    fun testLongHelpFlag() {
        val arguments = arrayOf("--help")

        val exitCode = CommandLineInterface().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("vgo")
        assertThat(output).contains("Options")
    }

    @Test
    fun testShortHelpFlag() {
        val arguments = arrayOf("-h")

        val exitCode = CommandLineInterface().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("vgo")
        assertThat(output).contains("Options")
    }

    @Test
    fun testLongStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "--stats")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun `unmodified files are omitted from statistics`() {
        val arguments =
            arrayOf(
                heartExampleRelativePath,
                "-o",
                "build/integrationTest/unmodified-stats-omitted.xml",
                "--stats",
            )
        val firstRunExitCode = CommandLineInterface().run(arguments)
        assertThat(firstRunExitCode).isEqualTo(0)

        val secondRunExitCode = CommandLineInterface().run(arguments)
        assertThat(secondRunExitCode).isEqualTo(0)

        val report = systemOutput.toString()
        assertThat(report).doesNotContain(heartExampleRelativePath)
    }

    @Test
    fun `directory inputs include a filename with statistics`() {
        val arguments =
            arrayOf(
                "src/test/resources",
                "-o",
                "build/integrationTest/multi-stats-test-directory",
                "--stats",
            )
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val report = systemOutput.toString()
        assertThat(report).contains(Paths.get("src/test/resources/avocado_example.xml").toString())
    }

    @Test
    fun `multiple file inputs include a filename with statistics`() {
        val arguments =
            arrayOf(
                avocadoExampleRelativePath,
                "-o",
                "build/integrationTest/multi-stats-test-one.xml",
                heartExampleRelativePath,
                "-o",
                "build/integrationTest/multi-stats-test-two.xml",
                "--stats",
            )
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val report = systemOutput.toString()
        assertThat(report).contains(avocadoExampleRelativePath)
        assertThat(report).contains(heartExampleRelativePath)
    }

    @Test
    fun testShortStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "-s")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testOverwritingInputFileReportsNonZeroSizeChange() {
        val overwritePath = Paths.get("build/integrationTest/stat-overwrite-test.xml")
        File(avocadoExampleRelativePath).copyTo(overwritePath.toFile(), overwrite = true)

        val arguments = arrayOf(overwritePath.toString(), "-s")
        val exitCode = CommandLineInterface().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
        assertThat(systemOutput.toString()).doesNotContain("Percent saved: 0.0")
    }

    @Test
    fun testIndentOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/indent-test.xml", "--indent", "4")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/indent-test.xml").readText()
        assertThat(output).contains("    <path")
    }

    @Test
    fun testFormatOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/format-test.svg", "--format", "svg")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/format-test.svg").readText()
        assertThat(output).startsWith("<svg")
    }

    @Test
    fun testNoOptimizationOption() {
        val arguments =
            arrayOf(
                avocadoExampleRelativePath,
                "-o",
                "build/integrationTest/no-optimization-test.svg",
                "--indent",
                "2",
                "--format",
                "svg",
                "--no-optimization",
            )
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/no-optimization-test.svg").readText()
        assertThat(output).startsWith("<svg")
        assertThat(output).hasLineCount(42)
    }

    @Test
    fun `print-ir default mode produces ir output`() {
        val arguments = arrayOf(heartExampleRelativePath, "--print-ir")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Path")
    }

    @Test
    fun `print-ir plain mode produces no ansi escape codes`() {
        val arguments = arrayOf(heartExampleRelativePath, "--print-ir=plain")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val esc = Char(0x1B).toString()
        assertThat(systemOutput.toString()).doesNotContain(esc)
    }

    @Test
    fun `print-ir color mode produces ansi escape codes`() {
        val arguments = arrayOf(heartExampleRelativePath, "--print-ir=color")
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val esc = Char(0x1B).toString()
        assertThat(systemOutput.toString()).contains(esc)
    }

    @Test
    fun `print-ir unknown mode warns and skips ir output`() {
        val arguments =
            arrayOf(
                heartExampleRelativePath,
                "-o",
                "build/integrationTest/print-ir-unknown.xml",
                "--print-ir=unknown",
            )
        val exitCode = CommandLineInterface().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemError.toString()).contains("unsupported ir dump mode")
        assertThat(systemOutput.toString()).doesNotContain("Path")
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun createTempDir() {
            File("build/integrationTest").apply {
                mkdir()
                deleteOnExit()
            }
        }
    }
}
