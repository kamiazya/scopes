plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    testImplementation(project(":platform:commons"))
    testImplementation(project(":platform:observability"))
    testImplementation(project(":platform:application-commons"))

    // Bounded Context - Unified Scope Management
    testImplementation(project(":contexts:scope-management:domain"))
    testImplementation(project(":contexts:scope-management:application"))
    testImplementation(project(":contexts:scope-management:infrastructure"))

    // Apps layer
    testImplementation(project(":apps:cli"))
    testImplementation(project(":apps:api"))
    testImplementation(project(":apps:daemon"))

    // Boot layer
    testImplementation(project(":boot:cli-launcher"))
    testImplementation(project(":boot:daemon-launcher"))

    // Test libraries
    testImplementation(libs.konsist)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}
