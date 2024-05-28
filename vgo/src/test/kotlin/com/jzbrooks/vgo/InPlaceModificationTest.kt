package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths

class InPlaceModificationTest {
    private lateinit var systemOutput: ByteArrayOutputStream

    @BeforeEach
    fun copyToSide() {
        val originalFolder = File("src/test/resources/in-place-modify/")
        val reservedFolder = File("build/test-results/inPlaceModification/reserved/")
        originalFolder.copyRecursively(reservedFolder, true)
    }

    @AfterEach
    fun resetFiles() {
        val originalFolder = File("src/test/resources/in-place-modify/")
        val reservedFolder = File("build/test-results/inPlaceModification/reserved/")
        reservedFolder.copyRecursively(originalFolder, true)
    }

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
    fun `in-place optimization completes successfully`() {
        val arguments = arrayOf("src/test/resources/in-place-modify")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun `individual file statistics are reported with a directory input`() {
        val arguments = arrayOf("src/test/resources/in-place-modify", "--stats")

        Application().run(arguments)

        assertThat(systemOutput.toString())
            .contains(Paths.get("src/test/resources/in-place-modify/avocado_example.xml").toString())
    }

    @Test
    fun `non-vector files are not mentioned in statistics reporting with a directory input`() {
        val arguments = arrayOf("src/test/resources/in-place-modify", "--stats")

        Application().run(arguments)

        assertThat(systemOutput.toString())
            .doesNotContain("src/test/resources/in-place-modify/non_vector.xml")
    }

    @Test
    fun `only modified files appear in statistics reporting`() {
        val arguments = arrayOf("src/test/resources/in-place-modify", "--stats")

        Application().run(arguments)

        assertThat(systemOutput.toString())
            .doesNotContain("src/test/resources/in-place-modify/avocado_example_optimized.xml")
    }

    @Test
    fun `non-vector files are not modified`() {
        val input = File("src/test/resources/in-place-modify/non_vector.xml")
        val before = input.readText()

        Application().run(arrayOf(input.parent))

        val after = input.readText()
        assertThat(after).isEqualTo(before)
    }
}
