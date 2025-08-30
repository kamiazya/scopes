plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    kotlin("plugin.serialization")
    alias(libs.plugins.sqldelight)
}
dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)

    // Database
    implementation(libs.sqlite.jdbc)

    // SQLDelight
    implementation(libs.sqldelight.driver.sqlite)
    implementation(libs.sqldelight.coroutines)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.arrow)
    testImplementation(libs.kotest.property)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

sqldelight {
    databases {
        create("ScopeManagementDatabase") {
            packageName.set("io.github.kamiazya.scopes.scopemanagement.db")
            dialect(libs.sqldelight.dialect.sqlite)
        }
    }
}
