plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.kotest.assertions.arrow)
}

tasks.test {
    useJUnitPlatform()
}
