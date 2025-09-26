import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources

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

    // Depend on all necessary tasks to fix Gradle dependency ordering issues
    dependsOn(subprojects.map { it.tasks.withType<Test>() })
    dependsOn(subprojects.map { it.tasks.withType<JacocoReport>() })
    dependsOn(subprojects.map { it.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() })
    dependsOn(subprojects.map { it.tasks.withType<JavaCompile>() })
    dependsOn(subprojects.map { it.tasks.withType<ProcessResources>() })
    
    // Explicitly depend on the problematic tasks mentioned in CI errors
    dependsOn(":apps-scopes:compileJava")
    dependsOn(":apps-scopes:compileKotlin")
    dependsOn(":apps-scopes:compileTestKotlin")
    dependsOn(":apps-scopes:test")
    dependsOn(":apps-scopes:jacocoTestReport")

    // Collect execution data from all subprojects
    executionData(
        fileTree(project.rootDir) {
            include("**/build/jacoco/*.exec")
        },
    )

    // Collect source directories from all subprojects
    sourceDirectories.setFrom(
        files(
            subprojects.flatMap { subproject ->
                listOf("${subproject.projectDir}/src/main/kotlin")
            },
        ),
    )

    // Collect class directories from all subprojects
    classDirectories.setFrom(
        files(
            subprojects.map { subproject ->
                fileTree("${subproject.buildDir}/classes/kotlin/main") {
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
