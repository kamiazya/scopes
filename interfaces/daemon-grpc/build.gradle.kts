plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    // Project dependencies
    implementation(project(":interfaces-rpc-contracts"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))

    // Contract dependencies for cross-context communication
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))
    implementation(project(":contracts-event-store"))
    implementation(project(":contracts-device-synchronization"))

    // Scope Management Context - for actual implementation
    implementation(project(":scope-management-application"))
    implementation(project(":scope-management-infrastructure"))

    // gRPC dependencies
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    // Kotlin
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

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
}
