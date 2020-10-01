package com.jzbrooks.vgo

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class InPlaceModificationTest {

    @BeforeEach
    fun copyToSide() {
        val originalFolder = File("src/integration-test/resources/in-place-modify/")
        val reservedFolder = File("build/integrationTest/inPlaceModification/reserved/")
        originalFolder.copyRecursively(reservedFolder, true)
    }

    @AfterEach
    fun resetFiles() {
        val originalFolder = File("src/integration-test/resources/in-place-modify/")
        val reservedFolder = File("build/integrationTest/inPlaceModification/reserved/")
        reservedFolder.copyRecursively(originalFolder, true)
    }

    @Test
    fun testInPlaceOptimizationCompletes() {
        val arguments = arrayOf("src/integration-test/resources/in-place-modify")

        val exitCode = Application().run(arguments)

        assertThat(exitCode).isEqualTo(0)
    }

    @Test
    fun testNonVectorLeftUntouched() {
        val input = File("src/integration-test/resources/in-place-modify/non_vector.xml")
        val before = input.readText()

        Application().run(arrayOf(input.parent))

        val after = input.readText()
        assertThat(after).isEqualTo(before)
    }
}