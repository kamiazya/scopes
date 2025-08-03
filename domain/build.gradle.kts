plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation(libs.kulid)

    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}
