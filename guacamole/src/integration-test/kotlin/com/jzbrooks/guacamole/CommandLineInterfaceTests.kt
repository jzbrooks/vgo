package com.jzbrooks.guacamole

import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Path

class CommandLineInterfaceTests {
    private val avocadoExampleRelativePath = "src/integration-test/resources/avocado_example.xml"
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

    // The version is baked into the jar manifest. It would be nice to be able to
    // remove the dependency on the jar task for the tests and to not lose the test
    // which would make running the tests with a plain junit runner easier

    @Test
    fun testLongVersionFlag() {
        val arguments = arrayOf("--version")
        val standardOutput = File("build/integrationTest/version.tmp").apply { deleteOnExit() }
        val process = ProcessBuilder("java", "-jar", "build/libs/debug/guacamole.jar", *arguments)
                .redirectOutput(standardOutput)
                .start()

        assertThat(process.waitFor()).isEqualTo(0)
        assertThat(standardOutput.readText()).matches(Regex("\\d+.\\d+.\\d+\n"))
    }

    @Test
    fun testShortVersionFlag() {
        val arguments = arrayOf("-v")
        val standardOutput = File("build/integrationTest/version.tmp").apply { deleteOnExit() }
        val process = ProcessBuilder("java", "-jar", "build/libs/debug/guacamole.jar", *arguments)
                .redirectOutput(standardOutput)
                .start()

        assertThat(process.waitFor()).isEqualTo(0)
        assertThat(standardOutput.readText()).matches(Regex("\\d+.\\d+.\\d+\n"))
    }

    @Test
    fun testLongHelpFlag() {
        val arguments = arrayOf("--help")

        val exitCode = Guacamole().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("guacamole")
        assertThat(output).contains("Options")
    }

    @Test
    fun testShortHelpFlag() {
        val arguments = arrayOf("-h")

        val exitCode = Guacamole().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("guacamole")
        assertThat(output).contains("Options")
    }

    @Test
    fun testLongStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "--stats")
        val exitCode = Guacamole().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testShortStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "-s")
        val exitCode = Guacamole().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testOverwritingInputFileReportsNonZeroSizeChange() {
        val overwritePath = Path.of("build/integrationTest/stat-overwrite-test.xml")
        File(avocadoExampleRelativePath).copyTo(overwritePath.toFile(), overwrite = true)

        val arguments = arrayOf(overwritePath.toString(), "-s")
        val exitCode = Guacamole().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
        assertThat(systemOutput.toString()).doesNotContain("Percent saved: 0.0")
    }

    @Test
    fun testIndentOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/indent-test.xml", "--indent", "4")
        val exitCode = Guacamole().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/indent-test.xml").readText()
        assertThat(output).contains("    <path")
    }

    @Test
    fun testFormatOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/format-test.svg", "--format", "svg")
        val exitCode = Guacamole().run(arguments)
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