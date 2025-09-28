plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    // Koin BOM for dependency management
    implementation(platform(libs.koin.bom))

    // Project dependencies
    implementation(project(":interfaces-rpc-contracts"))
    implementation(project(":contracts-scope-management"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-infrastructure"))

    // gRPC dependencies
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services) // For StatusProto and error details handling
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // DI
    implementation(libs.koin.core)

    // Arrow
    implementation(libs.arrow.core)

    // Logging
    implementation(libs.slf4j.api)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.koin.test)

    // gRPC Testing
    testImplementation(libs.grpc.testing)
    testImplementation(libs.grpc.inprocess)

    // Test dependency on daemon-grpc for TaskGatewayServiceImpl
    testImplementation(project(":interfaces-grpc-server-daemon"))
}

tasks.test {
    useJUnitPlatform()
}
