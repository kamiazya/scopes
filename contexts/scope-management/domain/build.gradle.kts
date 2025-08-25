plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-domain-commons"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
