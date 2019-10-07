package com.jzbrooks.guacamole

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.matches
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class CommandLineInterfaceTests {
    private val avocadoExampleRelativePath = "src/integration-test/resources/avocado_example.xml"
    private lateinit var systemOutput: ByteArrayOutputStream

    @Before
    fun redirect() {
        systemOutput = ByteArrayOutputStream()
        System.setOut(PrintStream(systemOutput))
    }

    @After
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
        val process = ProcessBuilder("java", "-jar", "build/libs/guacamole.jar", *arguments)
                .redirectOutput(standardOutput)
                .start()

        assertThat(process.waitFor()).isEqualTo(0)
        assertThat(standardOutput.readText()).matches(Regex("\\d+.\\d+.\\d+\n"))
    }

    @Test
    fun testShortVersionFlag() {
        val arguments = arrayOf("-v")
        val standardOutput = File("build/integrationTest/version.tmp").apply { deleteOnExit() }
        val process = ProcessBuilder("java", "-jar", "build/libs/guacamole.jar", *arguments)
                .redirectOutput(standardOutput)
                .start()

        assertThat(process.waitFor()).isEqualTo(0)
        assertThat(standardOutput.readText()).matches(Regex("\\d+.\\d+.\\d+\n"))
    }

    @Test
    fun testLongHelpFlag() {
        val arguments = arrayOf("--help")

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("guacamole")
        assertThat(output).contains("Options")
    }

    @Test
    fun testShortHelpFlag() {
        val arguments = arrayOf("-h")

        val exitCode = App().run(arguments)

        assertThat(exitCode).isEqualTo(0)
        val output = systemOutput.toString()
        assertThat(output).contains("guacamole")
        assertThat(output).contains("Options")
    }

    @Test
    fun testLongStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "--stats")
        val exitCode = App().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testShortStatsFlag() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/stats-test.xml", "-s")
        val exitCode = App().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        assertThat(systemOutput.toString()).contains("Percent saved:")
    }

    @Test
    fun testIndentOption() {
        val arguments = arrayOf(avocadoExampleRelativePath, "-o", "build/integrationTest/indent-test.xml", "--indent", "4")
        val exitCode = App().run(arguments)
        assertThat(exitCode).isEqualTo(0)
        val output = File("build/integrationTest/indent-test.xml").readText()
        assertThat(output).contains("    <path")
    }

    companion object {
        @BeforeClass
        fun createTempDir() {
            File("build/integrationTest").apply {
                mkdir()
                deleteOnExit()
            }
        }
    }
}