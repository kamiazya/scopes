plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":platform:commons"))
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
}
