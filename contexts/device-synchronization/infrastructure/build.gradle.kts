plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
}

dependencies {
    implementation(project(":device-synchronization-domain"))
    implementation(project(":device-synchronization-application"))
    implementation(project(":contracts-device-synchronization"))
    implementation(project(":contracts-event-store"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-infrastructure"))
    implementation(project(":platform-observability"))

    // DDD building blocks
    implementation(libs.jmolecules.ddd)

    // Database
    implementation(libs.sqlite.jdbc)

    // SQLDelight
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutines)

    // Functional programming
    implementation(libs.arrow.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Date/Time
    implementation(libs.kotlinx.datetime)

    // Dependency injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sqldelight {
    databases {
        create("DeviceSyncDatabase") {
            packageName.set("io.github.kamiazya.scopes.devicesync.db")
            dialect(libs.sqldelight.dialect.sqlite)
        }
    }
}
