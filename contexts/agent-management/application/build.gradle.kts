plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Domain layer
    implementation(project(":agent-management-domain"))

    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))

    // Contracts
    implementation(project(":contracts-agent-management"))

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
}

tasks.test {
    useJUnitPlatform()
}
