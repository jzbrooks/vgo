import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.FileInputStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

plugins {
    id("org.jetbrains.kotlin.jvm")
}

val buildProperties = run {
    val properties = Properties()
    FileInputStream("$projectDir/build.properties").use(properties::load)
    properties
}


val MAIN_CLASS = "com.jzbrooks.vgo.Application"
val VERSION = buildProperties["version"].toString()

sourceSets {
    main {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/generated/kotlin")
        }
    }

    create("integrationTest") {
        withConvention(KotlinSourceSet::class) {
            kotlin.srcDir("src/integration-test/kotlin")
            resources.srcDir("src/integration-test/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

dependencies {
    implementation(project(":vgo-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    withType<KotlinCompile<*>> {
        dependsOn("generateConstants")
    }

    val generateConstants by registering {
        doLast {
            val generatedDirectory = Paths.get("$projectDir/src/generated/kotlin/com/jzbrooks")
            Files.createDirectories(generatedDirectory)
            val generatedFile = generatedDirectory.resolve("BuildConstants.kt")
            PrintWriter(generatedFile.toFile()).use { output ->
                val buildConstantsClass = buildString {
                    appendln("""
                           |package com.jzbrooks
                           |                           
                           |object BuildConstants {
                           """.trimMargin())

                    for (property in buildProperties) {
                        append("    const val ")
                        append(property.key.toString().toUpperCase())
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


    withType<Jar> {
        destinationDirectory.set(file("$buildDir/libs/debug"))

        manifest {
            attributes["Main-Class"] = MAIN_CLASS
            attributes["Bundle-Version"] = VERSION
        }

        dependsOn(configurations.runtimeClasspath)
        from(configurations.runtimeClasspath.get().filter { it.isFile }.map(::zipTree))
    }

    val optimize by registering(JavaExec::class) {
        description = "Runs proguard on the jar application."
        group = "build"

        classpath = files("$rootDir/tools/r8.jar")
        args = listOf(
                "--release",
                "--classfile",
                "--lib", System.getenv("JAVA_HOME"),
                "--output", "$buildDir/libs/vgo.jar",
                "--pg-conf", "$rootDir/optimize.pro",
                "$buildDir/libs/debug/vgo.jar"
        )

        dependsOn(getByName("jar"))
    }

    val integrationTest by registering(Test::class) {
        description = "Runs the integration tests."
        group = "verification"
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        mustRunAfter("test")
    }

    val updateBaselineOptimizations by registering(Copy::class) {
        description = "Updates baseline assets with the latest integration test outputs."
        group = "Build Setup"

        from("$buildDir/integrationTest/") {
            include("*testOptimizationFinishes.xml")
            include("*testOptimizationFinishes.svg")
        }
        into("src/integration-test/resources/baseline/")
        rename { original ->
            val originalAssetName = original.let {
                val i = it.lastIndexOf('_')
                it.substring(0 until i)
            }
            val fileExtension = original.split('.').last()
            "${originalAssetName}_optimized.$fileExtension"
        }
    }

    getByName("check").dependsOn(integrationTest)
}
