plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${project.ext["kotlinSerialization"]}")
    implementation("com.github.guepardoapps:kulid:${project.ext["kulid"]}")

    testImplementation("io.kotest:kotest-runner-junit5:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-assertions-core:${project.ext["kotest"]}")
    testImplementation("io.kotest:kotest-property:${project.ext["kotest"]}")
}

tasks.test {
    useJUnitPlatform()
}
