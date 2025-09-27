import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
    jacoco
    alias(libs.plugins.jacoco.report.aggregation)
    alias(libs.plugins.sonarqube)
}

repositories {
    mavenCentral()
}

dependencies {
    jacocoAggregation(project(":platform-commons"))
    jacocoAggregation(project(":platform-application-commons"))
    jacocoAggregation(project(":platform-domain-commons"))
    jacocoAggregation(project(":platform-infrastructure"))
    jacocoAggregation(project(":platform-observability"))

    // Contracts
    jacocoAggregation(project(":contracts-scope-management"))
    jacocoAggregation(project(":contracts-user-preferences"))
    jacocoAggregation(project(":contracts-event-store"))
    jacocoAggregation(project(":contracts-device-synchronization"))

    // Scope Management Context
    jacocoAggregation(project(":scope-management-domain"))
    jacocoAggregation(project(":scope-management-application"))
    jacocoAggregation(project(":scope-management-infrastructure"))

    // User Preferences Context
    jacocoAggregation(project(":user-preferences-domain"))
    jacocoAggregation(project(":user-preferences-application"))
    jacocoAggregation(project(":user-preferences-infrastructure"))

    // Event Store Context
    jacocoAggregation(project(":event-store-domain"))
    jacocoAggregation(project(":event-store-application"))
    jacocoAggregation(project(":event-store-infrastructure"))

    // Device Synchronization Context
    jacocoAggregation(project(":device-synchronization-domain"))
    jacocoAggregation(project(":device-synchronization-application"))
    jacocoAggregation(project(":device-synchronization-infrastructure"))

    // Interfaces
    jacocoAggregation(project(":interfaces-cli"))
    jacocoAggregation(project(":interfaces-mcp"))

    // Apps
    jacocoAggregation(project(":apps-scopes"))
    jacocoAggregation(project(":apps-scopesd"))

    // Quality
    jacocoAggregation(project(":quality-konsist"))
}

// Create the aggregated report task directly
tasks.register<JacocoReport>("testCodeCoverageReport") {
    description = "Generate aggregated code coverage report for all modules"
    group = "verification"

    // Depend on test tasks from all subprojects with tests
    // Only depend on actual test and jacoco tasks to avoid spurious dependencies
    rootProject.subprojects.forEach { subproject ->
        subproject.tasks.findByName("test")?.let { dependsOn(it) }
        subproject.tasks.findByName("jacocoTestReport")?.let { dependsOn(it) }
    }

    // Collect execution data from all subprojects
    executionData.from(
        rootProject.subprojects
            .map { subproject ->
                "${subproject.layout.buildDirectory.get().asFile}/jacoco/test.exec"
            }.filter { file(it).exists() },
    )

    // Collect source directories from all subprojects
    sourceDirectories.setFrom(
        files(
            rootProject.subprojects.flatMap { subproject ->
                listOf("${subproject.projectDir}/src/main/kotlin")
            },
        ),
    )

    // Collect class directories from all subprojects
    classDirectories.setFrom(
        files(
            rootProject.subprojects.map { subproject ->
                fileTree("${subproject.layout.buildDirectory.get().asFile}/classes/kotlin/main") {
                    exclude("**/*Test.class", "**/*Spec.class")
                }
            },
        ),
    )

    // Configure report outputs
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.check {
    dependsOn(tasks.named("testCodeCoverageReport"))
}
