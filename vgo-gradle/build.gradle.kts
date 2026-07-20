plugins {
    id("vgo.kotlin-conventions")
    id("java-gradle-plugin")
    id("com.vanniktech.maven.publish")
}

dependencies {
    implementation(project(":vgo"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.assertk)
}

gradlePlugin {
    plugins {
        register("VgoPlugin") {
            id = "com.jzbrooks.vgo"
            displayName = "vgo"
            description = "Optimizes and converts vector artwork files."
            implementationClass = "com.jzbrooks.vgo.plugin.VgoPlugin"
        }
    }
}
