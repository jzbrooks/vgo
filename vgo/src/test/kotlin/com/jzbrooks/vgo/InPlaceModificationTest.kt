package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths

class InPlaceModificationTest {
    private lateinit var systemOutput: ByteArrayOutputStream

    @BeforeEach
    fun copyToSide(info: TestInfo) {
        val originalFolder = File("src/test/resources/in-place-modify/")
        val workingFolder = File("build/test-results/inPlaceModification/${info.displayName}/")
        originalFolder.copyRecursively(workingFolder, true)
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
    fun `in-place optimization completes successfully`(info: TestInfo) {
        val options =
            Vgo.Options(
                input = listOf("build/test-results/inPlaceModification/${info.displayName}/"),
            )
        val exitCode = Vgo(options).run()

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun `individual file statistics are reported with a directory input`(info: TestInfo) {
        val options =
            Vgo.Options(
                printStats = true,
                input = listOf("build/test-results/inPlaceModification/${info.displayName}/"),
            )
        Vgo(options).run()

        assertThat(systemOutput.toString())
            .contains(Paths.get("build/test-results/inPlaceModification/${info.displayName}/avocado_example.xml").toString())
    }

    @Test
    fun `non-vector files are not mentioned in statistics reporting with a directory input`(info: TestInfo) {
        val options =
            Vgo.Options(
                printStats = true,
                input = listOf("build/test-results/inPlaceModification/${info.displayName}"),
            )

        Vgo(options).run()

        assertThat(systemOutput.toString())
            .doesNotContain("build/test-results/inPlaceModification/${info.displayName}/non_vector.xml")
    }

    @Test
    fun `only modified files appear in statistics reporting`(info: TestInfo) {
        val options =
            Vgo.Options(
                printStats = true,
                input = listOf("build/test-results/inPlaceModification/${info.displayName}"),
            )

        Vgo(options).run()

        assertThat(systemOutput.toString())
            .doesNotContain("build/test-results/inPlaceModification/${info.displayName}/avocado_example_optimized.xml")
    }

    @Test
    fun `non-vector files are not modified`(info: TestInfo) {
        val input = File("build/test-results/inPlaceModification/${info.displayName}/non_vector.xml")
        val before = input.readText()

        val options =
            Vgo.Options(
                input = listOf(input.parent.toString()),
            )
        Vgo(options).run()

        val after = input.readText()
        assertThat(after).isEqualTo(before)
    }

    @Test
    fun `format option results in new file extension`(info: TestInfo) {
        val options =
            Vgo.Options(
                format = "svg",
                input = listOf("build/test-results/inPlaceModification/${info.displayName}"),
            )

        Vgo(options).run()

        assertThat(File("build/test-results/inPlaceModification/${info.displayName}/avocado_example.svg")).exists()
    }
}
