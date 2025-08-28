plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Domain layer dependency
    implementation(project(":scope-management-domain"))
    implementation(project(":platform-domain-commons"))

    // SQLite for local persistence
    implementation(libs.sqlite.jdbc)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Date/time
    implementation(libs.kotlinx.datetime)

    // Functional programming
    implementation(libs.arrow.core)

    // Logging
    implementation(libs.kermit)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
