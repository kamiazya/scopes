plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":contracts-scope-management"))
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.arrow.core)

    // CLI framework
    implementation(libs.clikt)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
}
