import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.2.1")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    }
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    apply<KtlintPlugin>()
    configure<KtlintExtension> {
        version.set("0.45.1")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
        }

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
