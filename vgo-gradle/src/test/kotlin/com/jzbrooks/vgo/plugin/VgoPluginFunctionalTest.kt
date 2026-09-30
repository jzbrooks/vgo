package com.jzbrooks.vgo.plugin

import assertk.assertThat
import assertk.assertions.contains
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

    @Test
    fun nestedProjectSourceResourcesAreNotConsumed(
        @TempDir projectDir: File,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "app"
            include(":ui-core")
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

        // Stands in for AGP's packageDebugResources, which consumes the library's
        // own hand-written res directory.
        projectDir.resolve("ui-core").mkdirs()
        projectDir.resolve("ui-core/build.gradle.kts").writeText(
            """
            abstract class PackageRes : DefaultTask() {
                @get:InputDirectory
                abstract val resDirectory: DirectoryProperty

                @get:OutputFile
                abstract val manifest: RegularFileProperty

                @TaskAction
                fun packageResources() {
                    manifest.get().asFile.writeText(
                        resDirectory.get().asFile.walkTopDown().filter { it.isFile }.joinToString { it.name }
                    )
                }
            }

            tasks.register<PackageRes>("packageRes") {
                resDirectory.set(layout.projectDirectory.dir("src/main/res"))
                manifest.set(layout.buildDirectory.file("packaged.txt"))
            }
            """.trimIndent(),
        )

        projectDir.resolve("ui-core/src/main/res/drawable").mkdirs()
        projectDir.resolve("ui-core/src/main/res/drawable/nested.xml").writeText(UNOPTIMIZED_DRAWABLE)
        projectDir.resolve("src/main/res/drawable").mkdirs()
        projectDir.resolve("src/main/res/drawable/icon.xml").writeText(UNOPTIMIZED_DRAWABLE)

        val result = runner(projectDir, "shrinkVectorGraphic", ":ui-core:packageRes").build()

        assertThat(result.output).doesNotContain("implicit dependency")
        assertThat(
            projectDir.resolve("ui-core/src/main/res/drawable/nested.xml").readText(),
        ).isEqualTo(UNOPTIMIZED_DRAWABLE)
    }

    @Test
    fun filesCopiedToTheOutputPathUnchangedAreReported(
        @TempDir projectDir: File,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "app"""")

        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                base
                id("com.jzbrooks.vgo")
            }

            vgo {
                outputs.setFrom(layout.buildDirectory.file("vgo/icon.xml"))
                indent = 2
            }
            """.trimIndent(),
        )

        // Already fully shrunk at this indentation, so the shrink task copies it.
        projectDir.resolve("src/main/res/drawable").mkdirs()
        projectDir.resolve("src/main/res/drawable/icon.xml").writeText(FIXED_POINT_DRAWABLE)

        val result = runner(projectDir, "shrinkVectorGraphic").build()

        assertThat(result.task(":shrinkVectorGraphic")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains(
            "Optimization did not shrink these files:",
            "\t" + listOf("src", "main", "res", "drawable", "icon.xml").joinToString(File.separator),
        )
        assertThat(projectDir.resolve("build/vgo/icon.xml").readText()).isEqualTo(FIXED_POINT_DRAWABLE)
    }

    private fun runner(
        projectDir: File,
        vararg tasks: String,
    ) = GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withTestKitDir(File("build/testkit").absoluteFile)
        .withPluginClasspath()
        .withArguments(*tasks, "--configuration-cache", "--info")

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
        private val FIXED_POINT_DRAWABLE =
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android" android:height="24dp" android:viewportHeight="24" android:viewportWidth="41" android:width="41dp">
              <path android:fillColor="#ff3008" android:pathData="M39.043,6.335a10.182,10.182,0,0,0,-9.008-5.429H1.558c-0.548-0-0.968,0.455-0.968,1.007 0,0.26 0.097,0.52 0.29,0.683L7.079,8.87a2.926,2.926,0,0,0,2.067,0.878h20.082c1.421,0 2.615,1.138 2.615,2.568 0,1.431-1.13,2.634-2.55,2.634H15.442c-0.549,0-0.969,0.455-0.969,1.007 0,0.26 0.097,0.521 0.291,0.683l6.166,6.274a2.926,2.926,0,0,0,2.067,0.878h6.263c8.169,0 14.336-8.777 9.783-17.457Z"/>
            </vector>
            """.trimIndent() + "\n"

        private val UNOPTIMIZED_DRAWABLE =
            """
            <vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
                <path android:fillColor="#FF000000" android:pathData="M 0.000 0.000 L 24.000 0.000 L 24.000 24.000 L 0.000 24.000 Z" />
            </vector>
            """.trimIndent()
    }
}
