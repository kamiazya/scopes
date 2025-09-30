plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Platform layer
    testImplementation(project(":platform-commons"))
    testImplementation(project(":platform-observability"))
    testImplementation(project(":platform-application-commons"))

    // Bounded Context - Unified Scope Management
    testImplementation(project(":scope-management-domain"))
    testImplementation(project(":scope-management-application"))
    testImplementation(project(":scope-management-infrastructure"))

    // Apps layer
    testImplementation(project(":apps-scopes"))
    testImplementation(project(":apps-scopesd"))

    // Test libraries
    testImplementation(libs.konsist)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)

    // ArchUnit and jMolecules integration
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.jmolecules.archunit)
    testImplementation(libs.jmolecules.ddd)
    testImplementation(libs.jmolecules.events)

    // JUnit Jupiter Engine for running JUnit5 tests (ArchUnit uses JUnit5)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()

    // Increase memory allocation for Konsist tests which analyze the entire codebase
    jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")

    // Set test timeout to prevent hanging
    systemProperty("kotest.framework.test.timeout", "300000") // 5 minutes in milliseconds
}
