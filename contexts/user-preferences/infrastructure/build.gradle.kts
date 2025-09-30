plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":user-preferences-domain"))
    implementation(project(":user-preferences-application"))
    implementation(project(":contracts-user-preferences"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-observability"))

    // DDD building blocks
    implementation(libs.jmolecules.ddd)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
