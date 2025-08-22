plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    implementation(project(":platform:commons"))
    implementation(project(":platform:observability"))
    implementation(project(":platform:application-commons"))

    // Bounded Context - Unified Scope Management
    implementation(project(":contexts:scope-management:application"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)

    // TODO: Add HTTP server framework (Ktor, Spring Boot, etc.)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
