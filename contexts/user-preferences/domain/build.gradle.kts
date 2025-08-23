plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}
