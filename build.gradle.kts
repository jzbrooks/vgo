import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.codearte.gradle.nexus.NexusStagingExtension

buildscript {
    repositories {
        jcenter()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.0.0")
        classpath("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.22.0")
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    group = "com.jzbrooks"
    version = properties["vgo_version"]?.toString() ?: ""

    repositories {
        mavenLocal()
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs = listOf("-Xinline-classes")
            useIR = true
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        addTestListener(object : TestListener {
            override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                if (suite.parent == null) {
                    val output = "|  Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)  |"
                    val border = "-".repeat(output.length)
                    println("""
                        $border
                        $output
                        $border
                    """.trimIndent()
                    )
                }
            }

            override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
            override fun beforeTest(testDescriptor: TestDescriptor?) {}
            override fun beforeSuite(suite: TestDescriptor?) {}
        })
    }
}

ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_KEY_FILE_PATH")

apply(plugin = "io.codearte.nexus-staging")
configure<NexusStagingExtension> {
    packageGroup = "com.jzbrooks"
    stagingProfileId = System.getenv("SONATYPE_PROFILE_ID")
    numberOfRetries = 60
    delayBetweenRetriesInMillis = 30_000
    username = System.getenv("OSSRH_USERNAME")
    password = System.getenv("OSSRH_PASSWORD")
}