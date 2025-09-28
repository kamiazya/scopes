plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":platform-commons"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))
    implementation(project(":platform-observability"))
    implementation(project(":interfaces-mcp"))
    implementation(project(":interfaces-grpc-client-daemon"))
    implementation(project(":interfaces-rpc-contracts"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.io.core)
    implementation(libs.arrow.core)

    // CLI framework
    implementation(libs.clikt)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // gRPC
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.services)
    implementation(libs.grpc.protobuf)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(project(":platform-infrastructure"))
    testImplementation(project(":platform-observability"))
    testImplementation(project(":scope-management-infrastructure"))
}

tasks.test {
    useJUnitPlatform()
}
