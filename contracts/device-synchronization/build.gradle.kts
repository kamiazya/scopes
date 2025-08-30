plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.bundles.kotest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
