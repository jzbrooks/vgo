plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
    id("org.gradle.kotlin.kotlin-dsl") version "4.4.0"
}

dependencies {
    implementation(project(":vgo"))

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.28.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(this@tasks["javadoc"])
    }
}
