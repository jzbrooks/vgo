plugins {
    id("vgo.kotlin-conventions")
}

val r8: Configuration = configurations.create("r8")

dependencies {
    implementation(project(":vgo"))
    implementation(project(":vgo-core"))

    implementation(libs.android.sdk.common)
    implementation(libs.kotlin.compiler.embeddable)

    r8(libs.r8)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.assertk)
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.cli.CommandLineInterface"
            attributes["Bundle-Version"] = providers.gradleProperty("VERSION_NAME").get()
        }

        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/*.EC",
            "META-INF/*.SF.*",
            "META-INF/*.DSA.*",
            "META-INF/*.RSA.*",
            "META-INF/*.EC.*",
            "META-INF/BCKEY.DSA",
            "META-INF/BC2048KE.DSA",
            "META-INF/BCKEY.SF",
            "META-INF/BC2048KE.SF",
            "**/*.kotlin_metadata",
            "**/*.kotlin_module",
            "**/*.kotlin_builtins",
            "**/module-info.class",
            "META-INF/maven/**",
            "META-INF/versions/**",
            "META-INF/*.version",
            "META-INF/LICENSE*",
            "META-INF/LGPL2.1",
            "META-INF/DEPENDENCIES",
            "META-INF/AL2.0",
            "**/NOTICE*",
            "javax/activation/**",
            "xsd/catalog.xml",
        )

        from(configurations.runtimeClasspath.map { classpath -> classpath.files.map(::zipTree) })

        destinationDirectory.set(layout.buildDirectory.dir("libs/debug"))
    }

    val optimize =
        register<JavaExec>("optimize") {
            description = "Runs r8 on the jar application."
            group = "build"

            inputs.file(layout.buildDirectory.file("libs/debug/vgo-cli.jar"))
            outputs.file(layout.buildDirectory.file("libs/vgo.jar"))

            val javaHome = System.getProperty("java.home")

            classpath(r8)
            mainClass = "com.android.tools.r8.R8"

            args(
                "--release",
                "--classfile",
                "--lib",
                javaHome,
                "--output",
                layout.buildDirectory.file("libs/vgo.jar").get(),
                "--pg-conf",
                "optimize.pro",
                layout.buildDirectory.file("libs/debug/vgo-cli.jar").get(),
            )

            dependsOn(jar)
        }

    register("binary") {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file(layout.buildDirectory.file("libs/vgo.jar"))
        outputs.file(layout.buildDirectory.file("libs/vgo"))

        doLast {
            val binaryFile = outputs.files.singleFile
            val optimizedJar = inputs.files.singleFile
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            optimizedJar.inputStream().use {
                binaryFile.appendBytes(it.readBytes())
            }
            binaryFile.setExecutable(true, false)
        }
    }
}
