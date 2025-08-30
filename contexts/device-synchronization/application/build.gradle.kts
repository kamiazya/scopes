plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":device-synchronization-domain"))
    implementation(project(":platform-application-commons"))
    implementation(project(":contracts-device-synchronization"))
    implementation(project(":contracts-event-store")) // For using EventStorePort

    // Functional programming
    implementation(libs.arrow.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Date/time
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
