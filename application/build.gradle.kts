plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${project.ext["kotlinCoroutines"]}")

    testImplementation("io.kotest:kotest-runner-junit5:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-assertions-core:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-property:${project.ext["kotest"]}")
    testImplementation("io.mockk:mockk:${project.ext["mockk"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${project.ext["kotlinCoroutines"]}")
}

tasks.test {
    useJUnitPlatform()
}
