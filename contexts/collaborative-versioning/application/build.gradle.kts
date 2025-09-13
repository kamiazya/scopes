plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Domain layer
    implementation(project(":collaborative-versioning-domain"))

    // Cross-context dependencies
    implementation(project(":agent-management-domain")) // For AgentId

    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-observability"))

    // Contracts
    implementation(project(":contracts-collaborative-versioning"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}
