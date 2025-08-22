plugins {
    kotlin("jvm")
}
dependencies {
    implementation(project(":platform:commons"))
    implementation(project(":platform:application-commons"))
    implementation(project(":contexts:scope-management:domain"))
    implementation(project(":contexts:scope-management:application"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kulid)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}
