plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
}

dependencies {
    implementation(project(":event-store-domain"))
    implementation(project(":event-store-application"))
    implementation(project(":contracts-event-store"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))

    // SQLite for local persistence
    implementation(libs.sqlite.jdbc)

    // SQLDelight
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutines)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Functional programming
    implementation(libs.arrow.core)

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
        create("EventStoreDatabase") {
            packageName.set("io.github.kamiazya.scopes.eventstore.db")
            dialect(libs.sqldelight.dialect.sqlite)
        }
    }
}
