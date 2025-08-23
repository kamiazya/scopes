plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))

    // Bounded Context - Unified Scope Management (Full stack for daemon)
    implementation(project(":scope-management-application"))
    implementation(project(":scope-management-infrastructure"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)

    // TODO: Add IPC/networking libraries

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
