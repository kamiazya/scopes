plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":contexts:scope-management:domain"))
    implementation(project(":contexts:scope-management:application"))
    implementation(project(":platform:commons"))
    implementation(project(":platform:application-commons"))
    implementation(project(":platform:observability"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
}
