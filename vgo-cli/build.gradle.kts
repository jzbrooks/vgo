plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        dependsOn(configurations.runtimeClasspath)
        manifest {
            attributes["Main-Class"] = "com.jzbrooks.vgo.cli.CommandLineInterface"
            attributes["Bundle-Version"] = project.properties["VERSION_NAME"]

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
        }

        val sourceClasses =
            sourceSets.main
                .get()
                .output.classesDirs
        inputs.files(sourceClasses)
        destinationDirectory.set(layout.buildDirectory.dir("libs/debug"))

        doFirst {
            from(files(sourceClasses))
            from(
                configurations.runtimeClasspath
                    .get()
                    .asFileTree.files
                    .map(::zipTree),
            )
        }
    }

    val optimize by registering(JavaExec::class) {
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

        dependsOn(getByName("jar"))
    }

    val binaryFileProp = layout.buildDirectory.file("libs/vgo")
    val binary by registering {
        description = "Prepends shell script in the jar to improve CLI"
        group = "build"

        dependsOn(optimize)

        inputs.file(layout.buildDirectory.file("libs/vgo.jar"))
        outputs.file(binaryFileProp)

        doLast {
            val binaryFile = binaryFileProp.get().asFile
            binaryFile.parentFile.mkdirs()
            binaryFile.delete()
            binaryFile.appendText("#!/bin/sh\n\nexec java \$JAVA_OPTS -jar \$0 \"\$@\"\n\n")
            layout.buildDirectory.file("libs/vgo.jar").get().asFile.inputStream().use {
                binaryFile.appendBytes(it.readBytes())
            }
            binaryFile.setExecutable(true, false)
        }
    }
}

val r8: Configuration by configurations.creating

dependencies {
    implementation(project(":vgo"))

    implementation("com.android.tools:sdk-common:32.0.1")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.3.0")

    r8("com.android.tools:r8:8.13.19")

    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}
