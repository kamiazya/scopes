plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}
dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":scope-management-domain"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}
