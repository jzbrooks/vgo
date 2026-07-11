plugins {
    id("vgo.kotlin-conventions")
    id("com.vanniktech.maven.publish")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()
}

dependencies {
    implementation(project(":vgo-core"))

    // Provided by the android gradle plugin
    compileOnly(libs.android.sdk.common)

    // Provided by kotlin gradle plugin
    compileOnly(libs.kotlin.compiler.embeddable)

    implementation(libs.kotlinpoet)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.kotlin.compiler.embeddable)

    testImplementation(libs.assertk)
}

val generateConstants =
    tasks.register("generateConstants") {
        val constants =
            providers.gradlePropertiesPrefixedBy("generate.").get() +
                listOf("VERSION_NAME").associateWith { key -> providers.gradleProperty(key).get() }
        inputs.property("constants", constants)

        val outputDirectory = layout.buildDirectory.dir("generated/sources/buildConstants/kotlin")
        outputs.dir(outputDirectory)

        doLast {
            val packageDirectory = outputDirectory.get().asFile.resolve("com/jzbrooks")
            packageDirectory.mkdirs()

            val buildConstantsClass =
                buildString {
                    appendLine("package com.jzbrooks")
                    appendLine()
                    appendLine("internal object BuildConstants {")

                    for (constant in constants) {
                        append("    const val ")
                        append(constant.key.uppercase())
                        append(" = \"")
                        append(constant.value)
                        appendLine('"')
                    }

                    appendLine("}")
                }

            packageDirectory.resolve("BuildConstants.kt").writeText(buildConstantsClass)
        }
    }

kotlin.sourceSets["main"].kotlin.srcDir(generateConstants)

tasks.register<Copy>("updateBaselineOptimizations") {
    description = "Updates baseline assets with the latest integration test outputs."
    group = "Build Setup"

    val source = layout.buildDirectory.dir("test-results")
    from(source) {
        include("*testOptimizationFinishes.xml")
        include("*testOptimizationFinishes.svg")
        include("*testImageVectorIsEquivalentToBaseline.kt")
    }
    into("src/test/resources/baseline/")
    rename("(.+)_test(?:OptimizationFinishes|ImageVectorIsEquivalentToBaseline)\\.(xml|svg|kt)", "$1_optimized.$2")
}
