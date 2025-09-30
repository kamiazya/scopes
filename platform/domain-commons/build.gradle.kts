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

    // DDD building blocks (api() for transitive visibility)
    api(libs.jmolecules.ddd)
    api(libs.jmolecules.events)
    api(libs.jmolecules.layered.architecture)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
