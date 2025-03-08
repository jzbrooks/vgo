import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

kotlin.sourceSets
    .getByName("main")
    .kotlin
    .srcDir("src/generated/kotlin")

dependencies {
    implementation(project(":vgo-core"))

    compileOnly("com.android.tools:sdk-common:31.9.0")

    testImplementation(platform("org.junit:junit-bom:5.12.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

tasks {
    compileKotlin {
        dependsOn("generateConstants")
    }

    val generateConstants by registering {
        finalizedBy("compileKotlin")

        outputs.files("$projectDir/src/generated/kotlin/com/jzbrooks/BuildConstants.kt")

        doLast {
            val generatedDirectory = Paths.get("$projectDir/src/generated/kotlin/com/jzbrooks")
            Files.createDirectories(generatedDirectory)
            val generatedFile = generatedDirectory.resolve("BuildConstants.kt")

            PrintWriter(generatedFile.toFile()).use { output ->
                val buildConstantsClass =
                    buildString {
                        appendLine(
                            """
                               |package com.jzbrooks
                               |
                               |internal object BuildConstants {
                            """.trimMargin(),
                        )

                        val vgoProperties =
                            project.properties
                                .filterKeys { it == "VERSION_NAME" }

                        for (property in vgoProperties) {
                            append("    const val ")
                            append(property.key.uppercase())
                            append(" = \"")
                            append(property.value)
                            appendLine('"')
                        }

                        appendLine("}")
                    }
                output.write(buildConstantsClass)
            }
        }
    }

    withType<KtLintCheckTask>().configureEach {
        mustRunAfter(generateConstants)
    }

    withType<KtLintFormatTask>().configureEach {
        mustRunAfter(generateConstants)
    }

    val updateBaselineOptimizations by registering(Copy::class) {
        description = "Updates baseline assets with the latest integration test outputs."
        group = "Build Setup"

        val source = layout.buildDirectory.dir("test-results")
        from(source) {
            include("*testOptimizationFinishes.xml")
            include("*testOptimizationFinishes.svg")
        }
        into("src/test/resources/baseline/")
        rename("(\\w+)_testOptimizationFinishes.(xml|svg)", "$1_optimized.$2")
    }

    val sourcesJar by registering(Jar::class) {
        dependsOn(generateConstants)

        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by registering(Jar::class) {
        dependsOn(generateConstants)

        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
