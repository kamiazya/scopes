plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
