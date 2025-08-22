plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":contexts:scope-management:domain"))
    implementation(project(":contexts:scope-management:application"))
    implementation(project(":platform:commons"))
    implementation(project(":platform:application-commons"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
}
