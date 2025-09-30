plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":event-store-domain"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":contracts-event-store"))

    // DDD building blocks
    implementation(libs.jmolecules.ddd)

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
