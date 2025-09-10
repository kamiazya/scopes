plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
}

dependencies {
    // Domain and Application layers
    implementation(project(":collaborative-versioning-domain"))
    implementation(project(":collaborative-versioning-application"))

    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))
    implementation(project(":platform-observability"))

    // Contracts
    implementation(project(":contracts-collaborative-versioning"))
    implementation(project(":contracts-event-store"))

    // Event Store integration
    implementation(project(":event-store-domain"))
    implementation(project(":event-store-application"))

    // Agent management integration
    implementation(project(":agent-management-domain"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)
    implementation(libs.kotlinx.serialization.json)
    // Logging is provided by platform-observability

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Database
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqlite.jdbc)

    // Dependency Injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Test dependencies
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqldelight.driver.sqlite)
}

sqldelight {
    databases {
        create("CollaborativeVersioningDatabase") {
            packageName.set("io.github.kamiazya.scopes.collaborativeversioning.db")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
