plugins {
    kotlin("jvm")
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

    // gRPC dependencies (for endpoint resolution and channel building)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.stub)

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
