plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

dependencies {
    // Platform
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))

    // Contracts
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))

    // MCP SDK
    implementation(libs.mcp.kotlin.sdk)
    // For stdio transport no engine is needed, but keep Ktor server available if required by SDK
    implementation(libs.ktor.server.netty)
    // Okio (optional), and kotlinx-io for Source/Sink bridges
    implementation(libs.okio)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.io.core.jvm)

    // KotlinX
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)

    // Tests
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mcp.kotlin.sdk) // For InMemoryTransport and Client
}

tasks.test {
    useJUnitPlatform()
}
