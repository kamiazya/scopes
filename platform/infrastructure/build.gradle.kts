plugins {
    kotlin("jvm")
    alias(libs.plugins.sqldelight)
}

dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-observability"))

    implementation(libs.arrow.core)
    implementation(libs.kulid)

    api(libs.sqlite.jdbc)
    api(libs.sqldelight.driver.sqlite)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)

    // Dependency injection
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sqldelight {
    databases {
        create("PlatformDatabase") {
            packageName.set("io.github.kamiazya.scopes.platform.db")
            dialect(libs.sqldelight.dialect.sqlite)
        }
    }
}
