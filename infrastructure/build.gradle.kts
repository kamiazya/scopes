plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation("io.kotest:kotest-runner-junit5:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-assertions-core:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-property:${project.ext["kotest"]}")
    testImplementation("io.mockk:mockk:${project.ext["mockk"]}")
}

tasks.test {
    useJUnitPlatform()
}
