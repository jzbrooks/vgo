package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Paths

class VgoTests {
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
    fun `in-place individual file statistics are reported with a directory input`(info: TestInfo) {
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
    fun `in-place non-vector files are not mentioned in statistics reporting with a directory input`(info: TestInfo) {
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
    fun `in-place only modified files appear in statistics reporting`(info: TestInfo) {
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
    fun `in-place non-vector files are not modified`(info: TestInfo) {
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
    fun `in-place format option results in new file extension`(info: TestInfo) {
        val options =
            Vgo.Options(
                format = "svg",
                input = listOf("build/test-results/inPlaceModification/${info.displayName}"),
            )

        Vgo(options).run()

        assertThat(File("build/test-results/inPlaceModification/${info.displayName}/avocado_example.svg")).exists()
    }

    @Test
    fun `output-specified file extension is not overwritten by conversion format`(info: TestInfo) {
        val options =
            Vgo.Options(
                format = "svg",
                input = listOf("build/test-results/inPlaceModification/${info.displayName}/avocado_example.xml"),
                output = listOf("build/test-results/inPlaceModification/${info.displayName}/avocado_example.vec"),
            )

        Vgo(options).run()

        assertThat(File("build/test-results/inPlaceModification/${info.displayName}/avocado_example.vec")).exists()
        assertThat(File("build/test-results/inPlaceModification/${info.displayName}/avocado_example.svg"))
            .transform("exists") {
                it.exists()
            }.isFalse() // todo: replace this with doesNotExists() when assertk is updated
    }

    @Test
    fun `sequential runs on optimized files don't change the file unless the size is smaller`() {
        val targetPath = "build/test-results/bug_117.xml"

        File("src/test/resources/bug_117.xml")
            .copyTo(File(targetPath), overwrite = true)

        val startTime = File(targetPath).lastModified()

        val options =
            Vgo.Options(
                input = listOf(targetPath),
                indent = 2,
            )

        Vgo(options).run()

        val lastModification = File(targetPath).lastModified()

        assertThat(lastModification, "lastModification").isEqualTo(startTime)
    }
}
