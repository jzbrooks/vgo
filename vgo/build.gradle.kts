import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

sourceSets {
    main {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/generated/kotlin")
        }
    }
}

dependencies {
    implementation(project(":vgo-core"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.24")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
        destinationDirectory.set(file("$buildDir/libs/debug"))

        doFirst {
            from(files(sourceClasses))
            from(configurations.runtimeClasspath.get().asFileTree.files.map(::zipTree))

            exclude(
                "**/*.kotlin_metadata",
                "**/*.kotlin_module",
                "**/*.kotlin_builtins",
                "**/module-info.class",
                "META-INF/maven/**",
                "META-INF/*.version"
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
                val buildConstantsClass = buildString {
                    appendln(
                        """
                               |package com.jzbrooks
                               |
                               |internal object BuildConstants {
                               """.trimMargin()
                    )

                    val vgoProperties = project.properties
                        .filterKeys { it == "VERSION_NAME" }

                    for (property in vgoProperties) {
                        append("    const val ")
                        append(property.key.toUpperCase())
                        append(" = \"")
                        append(property.value)
                        appendln('"')
                    }

                    appendln("}")
                }
                output.write(buildConstantsClass)
            }
        }
    }

    val optimizedJar = file("$buildDir/libs/vgo.jar")
    val optimize by registering(JavaExec::class) {
        description = "Runs proguard on the jar application."
        group = "build"

        inputs.file("$buildDir/libs/debug/vgo-$version.jar")
        outputs.file(optimizedJar)

        val javaHome = System.getProperty("java.home")

        classpath = files("$rootDir/tools/r8.jar")
        args = listOf(
            "--release",
            "--classfile",
            "--lib", javaHome,
            "--output", "$buildDir/libs/vgo.jar",
            "--pg-conf", "$rootDir/optimize.pro",
            "$buildDir/libs/debug/vgo-$version.jar"
        )

        dependsOn(getByName("jar"))
    }

    val binaryFile = file("$buildDir/libs/vgo")
    val binary by registering {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file(optimizedJar)
        outputs.file(binaryFile)

        doLast {
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            optimizedJar.inputStream().use { binaryFile.appendBytes(it.readBytes()) }
            binaryFile.setExecutable(true, false)
        }
    }

    val updateBaselineOptimizations by registering(Copy::class) {
        description = "Updates baseline assets with the latest integration test outputs."
        group = "Build Setup"

        from("$buildDir/test-results/") {
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
