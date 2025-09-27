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

// Configure reports using the jacoco-report-aggregation plugin syntax
reporting {
    reports {
        val testCodeCoverageReport by creating(JacocoCoverageReport::class) {
            // This task will aggregate coverage from all projects specified in jacocoAggregation dependencies
            testSuiteName.set("test")
        }
    }
}
