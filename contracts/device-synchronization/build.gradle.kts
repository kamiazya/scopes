plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.kotlinx.datetime)

    // jMolecules architecture annotations
    api(libs.jmolecules.hexagonal.architecture)

    // Testing
    testImplementation(libs.bundles.kotest)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
