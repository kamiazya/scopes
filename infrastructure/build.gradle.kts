plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation(libs.kulid)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
