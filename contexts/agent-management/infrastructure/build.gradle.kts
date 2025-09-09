plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
}

dependencies {
    // Domain and Application layers
    implementation(project(":agent-management-domain"))
    implementation(project(":agent-management-application"))

    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-infrastructure"))

    // Contracts
    implementation(project(":contracts-agent-management"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)

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
        create("AgentManagementDatabase") {
            packageName.set("io.github.kamiazya.scopes.agentmanagement.db")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
