import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

kotlin.sourceSets.getByName("main").kotlin.srcDir("src/generated/kotlin")

val r8: Configuration by configurations.creating

dependencies {
    implementation(project(":vgo-core"))
    implementation("com.android.tools:sdk-common:31.5.1")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    r8("com.android.tools:r8:8.3.37")
}

tasks {
    compileKotlin {
        dependsOn("generateConstants")
    }

    jar {
        dependsOn(configurations.runtimeClasspath)
        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.Application"
            attributes["Bundle-Version"] = project.properties["VERSION_NAME"]
        }

        val sourceClasses = sourceSets.main.get().output.classesDirs
        inputs.files(sourceClasses)
        destinationDirectory.set(layout.buildDirectory.dir("libs/debug"))

        doFirst {
            from(files(sourceClasses))
            from(configurations.runtimeClasspath.get().asFileTree.files.map(::zipTree))

            exclude(
                "**/*.kotlin_metadata",
                "**/*.kotlin_module",
                "**/*.kotlin_builtins",
                "**/module-info.class",
                "META-INF/maven/**",
                "META-INF/*.version",
                "META-INF/LICENSE*",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/AL2.0",
                "META-INF/BCKEY.DSA",
                "META-INF/BC2048KE.DSA",
                "META-INF/BCKEY.SF",
                "META-INF/BC2048KE.SF",
                "**/NOTICE*",
                "javax/activation/**",
                "xsd/catalog.xml",
            )
        }
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

    val optimize by registering(JavaExec::class) {
        description = "Runs proguard on the jar application."
        group = "build"

        inputs.file("build/libs/debug/vgo.jar")
        outputs.file("build/libs/vgo.jar")

        val javaHome = System.getProperty("java.home")

        classpath(r8)
        mainClass = "com.android.tools.r8.R8"

        args(
            "--release",
            "--classfile",
            "--lib",
            javaHome,
            "--output",
            "build/libs/vgo.jar",
            "--pg-conf",
            "$rootDir/optimize.pro",
            "build/libs/debug/vgo.jar",
        )

        dependsOn(getByName("jar"))
    }

    val binaryFileProp = layout.buildDirectory.file("libs/vgo")
    val binary by registering {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file("build/libs/vgo.jar")
        outputs.file(binaryFileProp)

        doLast {
            val binaryFile = binaryFileProp.get().asFile
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            file("build/libs/vgo.jar").inputStream().use { binaryFile.appendBytes(it.readBytes()) }
            binaryFile.setExecutable(true, false)
        }
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

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
