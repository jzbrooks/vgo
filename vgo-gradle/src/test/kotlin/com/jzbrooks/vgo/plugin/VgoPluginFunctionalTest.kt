package com.jzbrooks.vgo.plugin

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Exercises the plugin's default configuration in a nested build, where a
 * subproject generates vector drawables into its build directory.
 */
class VgoPluginFunctionalTest {
    @Test
    fun generatedResourcesInNestedProjectsAreNotConsumed(
        @TempDir projectDir: File,
    ) {
        writeNestedBuild(projectDir)

        val handWritten = File(projectDir, "src/main/res/drawable/icon.xml")
        val generated = File(projectDir, "lib/build/generated/res/drawable/generated.xml")

        // Generate first so the resource exists on disk when the shrink task
        // resolves its inputs in the second build.
        val generateResult = runner(projectDir, ":lib:generateRes").build()
        assertThat(generateResult.task(":lib:generateRes")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        // Both tasks in one graph is what lets Gradle attribute the generated
        // resource to its producer.
        val shrinkResult = runner(projectDir, ":lib:generateRes", "shrinkVectorGraphic").build()

        assertThat(shrinkResult.output).doesNotContain("implicit dependency")
        assertThat(shrinkResult.task(":shrinkVectorGraphic")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(handWritten.readText()).isNotEqualTo(UNOPTIMIZED_DRAWABLE)
        assertThat(generated.readText()).isEqualTo(UNOPTIMIZED_DRAWABLE)
    }

    private fun runner(
        projectDir: File,
        vararg tasks: String,
    ) = GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withTestKitDir(File("build/testkit").absoluteFile)
        .withPluginClasspath()
        .withArguments(*tasks, "--configuration-cache")

    private fun writeNestedBuild(projectDir: File) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "root"
            include(":lib")
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                base
                id("com.jzbrooks.vgo")
            }
            """.trimIndent(),
        )

        projectDir.resolve("lib").mkdirs()
        projectDir.resolve("lib/build.gradle.kts").writeText(
            """
            abstract class GenerateRes : DefaultTask() {
                @get:OutputDirectory
                abstract val outputDirectory: DirectoryProperty

                @TaskAction
                fun generate() {
                    val directory = outputDirectory.get().asFile
                    directory.mkdirs()
                    directory.resolve("generated.xml").writeText(
                        ${"\"\"\""}$UNOPTIMIZED_DRAWABLE${"\"\"\""}
                    )
                }
            }

            tasks.register<GenerateRes>("generateRes") {
                outputDirectory.set(layout.buildDirectory.dir("generated/res/drawable"))
            }
            """.trimIndent(),
        )

        projectDir.resolve("src/main/res/drawable").mkdirs()
        projectDir.resolve("src/main/res/drawable/icon.xml").writeText(UNOPTIMIZED_DRAWABLE)
    }

    companion object {
        private val UNOPTIMIZED_DRAWABLE =
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
                <path android:fillColor="#FF000000" android:pathData="M 0.000 0.000 L 24.000 0.000 L 24.000 24.000 L 0.000 24.000 Z" />
            </vector>
            """.trimIndent()
    }
}
