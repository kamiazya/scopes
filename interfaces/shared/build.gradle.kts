plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-observability"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
}
