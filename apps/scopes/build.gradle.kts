plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.spdx.sbom)
    application
}

dependencies {
    // Platform layer
    implementation(project(":platform-commons"))
    implementation(project(":platform-domain-commons"))
    implementation(project(":platform-observability"))
    implementation(project(":platform-application-commons"))
    implementation(project(":platform-infrastructure"))

    // Contracts layer
    implementation(project(":contracts-scope-management"))
    implementation(project(":contracts-user-preferences"))

    // Interface layer
    implementation(project(":interfaces-cli"))
    implementation(project(":interfaces-mcp"))

    // Bounded Contexts - scope-management
    // Infrastructure dependencies are required here for the Composition Root pattern.
    // See docs/architecture/dependency-injection.md for details.
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
    implementation(libs.kotlinx.io.core)
    implementation(libs.arrow.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

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

// Shadow JAR configuration for fat JAR distribution
tasks.shadowJar {
    archiveBaseName.set("scopes")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    manifest {
        attributes["Main-Class"] = "io.github.kamiazya.scopes.apps.cli.MainKt"
        attributes["Multi-Release"] = "true"
        attributes["Implementation-Title"] = "Scopes"
        attributes["Implementation-Version"] = project.version.toString()
    }

    // Merge service files properly (important for DI frameworks like Koin)
    mergeServiceFiles()

    // Minimize JAR size by excluding unnecessary files
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
}

// Make build depend on shadowJar for easy testing
tasks.named("build") {
    dependsOn(tasks.shadowJar)
}
