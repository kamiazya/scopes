plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-commons"))

    // Functional programming
    implementation(libs.arrow.core)

    // Date/time
    implementation(libs.kotlinx.datetime)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
