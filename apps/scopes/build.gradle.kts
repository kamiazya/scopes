plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.graalvm.native)
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.spdx.sbom") version "0.9.0"
    application
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))

    // Contracts layer
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))

    // Interface layer
    implementation(project(":interfaces-cli"))

    // Bounded Contexts - scope-management
    // TODO: Remove infrastructure dependency - currently needed for DI configuration
    // This is a known architectural exception that needs to be addressed
    implementation(project(":scope-management-domain"))
    implementation(project(":scope-management-application"))
    implementation(project(":scope-management-infrastructure"))

    // Bounded Contexts - user-preferences
    implementation(project(":user-preferences-domain"))
    implementation(project(":user-preferences-application"))
    implementation(project(":user-preferences-infrastructure"))

    // Bounded Contexts - event-store
    implementation(project(":event-store-domain"))
    implementation(project(":event-store-application"))
    implementation(project(":event-store-infrastructure"))

    // Bounded Contexts - device-synchronization
    implementation(project(":device-synchronization-domain"))
    implementation(project(":device-synchronization-application"))
    implementation(project(":device-synchronization-infrastructure"))
    implementation(project(":contracts-event-store"))
    implementation(project(":contracts-device-synchronization"))

    // TODO: Enable when implemented
    // implementation(project(":contexts:aspect-management:application"))
    // implementation(project(":contexts:alias-management:application"))
    // implementation(project(":contexts:context-management:application"))

    // CLI framework
    implementation(libs.clikt)

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    // GraalVM native image
    compileOnly(libs.graalvm.sdk)

    // Logging
    runtimeOnly(libs.logback.classic)

    // Testing
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopes")
            mainClass.set("io.github.kamiazya.scopes.apps.cli.MainKt")
            useFatJar.set(true)

            buildArgs.addAll(
                listOf(
                    "-O2",
                    "--no-fallback",
                    "--gc=serial",
                    "--report-unsupported-elements-at-runtime",
                    "-H:+UnlockExperimentalVMOptions",
                    "-H:+ReportExceptionStackTraces",
                    "-H:+InstallExitHandlers",
                    "--initialize-at-build-time=kotlin",
                    "--initialize-at-build-time=kotlinx.coroutines",
                    "--initialize-at-run-time=kotlin.uuid.SecureRandomHolder",
                    "--initialize-at-run-time=org.sqlite",
                    "-Dorg.sqlite.lib.exportPath=${layout.buildDirectory.get()}/native/nativeCompile",
                    "--exclude-config",
                    ".*sqlite-jdbc.*\\.jar",
                    ".*native-image.*",
                    "-H:ResourceConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/resource-config.json",
                    "-H:ReflectionConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/reflect-config.json",
                    "-H:JNIConfigurationFiles=${layout.buildDirectory.get()}/resources/main/META-INF/native-image/jni-config.json",
                ),
            )
        }
    }

    toolchainDetection.set(false)
}

// Make nativeCompile depend on checkGraalVM
tasks.named("nativeCompile") {
    dependsOn(":checkGraalVM")
}
