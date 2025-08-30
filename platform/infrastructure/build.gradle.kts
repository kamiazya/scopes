plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-domain-commons"))

    implementation(libs.arrow.core)

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
}

tasks.withType<Test> {
    useJUnitPlatform()
}
