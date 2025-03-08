plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
    id("org.gradle.kotlin.kotlin-dsl") version "6.1.0"
}

dependencies {
    implementation(project(":vgo"))

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
}

gradlePlugin {
    plugins {
        register("VgoPlugin") {
            id = "com.jzbrooks.vgo"
            implementationClass = "com.jzbrooks.vgo.plugin.VgoPlugin"
        }
    }
}

tasks {
    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    })

    val javadocJar by registering(Jar::class) {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
