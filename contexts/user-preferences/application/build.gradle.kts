plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":user-preferences-domain"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))

    // DDD building blocks
    implementation(libs.jmolecules.ddd)

    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
