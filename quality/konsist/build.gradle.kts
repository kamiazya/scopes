plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    testImplementation(project(":platform-commons"))
    testImplementation(project(":platform-observability"))
    testImplementation(project(":platform-application-commons"))

    // Bounded Context - Unified Scope Management
    testImplementation(project(":scope-management-domain"))
    testImplementation(project(":scope-management-application"))
    testImplementation(project(":scope-management-infrastructure"))

    // Apps layer
    testImplementation(project(":apps-scopes"))
    testImplementation(project(":apps-scopesd"))

    // Test libraries
    testImplementation(libs.konsist)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
