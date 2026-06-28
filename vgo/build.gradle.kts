import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()

    sourceSets
        .getByName("main")
        .kotlin
        .srcDir("src/generated/kotlin")
}

dependencies {
    implementation(project(":vgo-core"))

    // Provided by the android gradle plugin
    compileOnly("com.android.tools:sdk-common:32.2.1")

    // Provided by kotlin gradle plugin
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")

    implementation("com.squareup:kotlinpoet:2.3.0")

    testImplementation(platform("org.junit:junit-bom:6.1.1"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.4.0")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

tasks {
    compileKotlin {
        dependsOn("generateConstants")
    }

    val generateConstants =
        register("generateConstants") {
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
                                providers.gradlePropertiesPrefixedBy("generate.").get() +
                                    listOf(
                                        "VERSION_NAME",
                                    ).associateWith { key -> providers.gradleProperty(key).get() }

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

    register<Copy>("updateBaselineOptimizations") {
        description = "Updates baseline assets with the latest integration test outputs."
        group = "Build Setup"

        val source = layout.buildDirectory.dir("test-results")
        from(source) {
            include("*testOptimizationFinishes.xml")
            include("*testOptimizationFinishes.svg")
            include("*testOptimizationFinishes.kt")
        }
        into("src/test/resources/baseline/")
        rename("(\\w+)_testOptimizationFinishes.(xml|svg|kt)", "$1_optimized.$2")
    }

    register<Jar>("sourcesJar") {
        dependsOn(generateConstants)

        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    register<Jar>("javadocJar") {
        dependsOn(generateConstants)

        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
