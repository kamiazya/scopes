pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "4.1.1"
}

rootProject.name = "scopes"

include(
    // Platform layer
    ":platform-commons",
    ":platform-observability",
    ":platform-application-commons",
    ":platform-domain-commons",
    ":platform-infrastructure",
    // Interface layer
    ":interfaces-cli",
    // Contracts layer
    ":contracts-scope-management",
    ":contracts-user-preferences",
    ":contracts-event-store",
    ":contracts-device-synchronization",
    ":contracts-collaborative-versioning",
    ":contracts-agent-management",
    // Bounded Context - Unified Scope Management
    ":scope-management-domain",
    ":scope-management-application",
    ":scope-management-infrastructure",
    // Bounded Context - User Preferences
    ":user-preferences-domain",
    ":user-preferences-application",
    ":user-preferences-infrastructure",
    // Bounded Context - Event Store
    ":event-store-domain",
    ":event-store-application",
    ":event-store-infrastructure",
    // Bounded Context - Device Synchronization
    ":device-synchronization-domain",
    ":device-synchronization-application",
    ":device-synchronization-infrastructure",
    // Bounded Context - Collaborative Versioning
    ":collaborative-versioning-domain",
    ":collaborative-versioning-application",
    ":collaborative-versioning-infrastructure",
    // Bounded Context - Agent Management
    ":agent-management-domain",
    ":agent-management-application",
    ":agent-management-infrastructure",
    // Apps layer - Application logic & Entry points
    ":apps-scopes",
    ":apps-scopesd",
    // Quality
    ":quality-konsist",
)

// Configure Gradle Build Scan
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"

        val isCi = System.getenv("CI") != null

        // Publish build scans for CI builds and main branch
        publishing.onlyIf {
            val isMainBranch = System.getenv("GITHUB_REF_NAME") in listOf("main", "master")
            (isCi || isMainBranch) && !gradle.startParameter.isOffline
        }

        // Disable background upload in CI for reliability
        uploadInBackground = !isCi

        // Capture build insights
        capture {
            buildLogging = true
            testLogging = true
        }

        // Add environment info
        tag(if (isCi) "CI" else "LOCAL")

        if (isCi) {
            // Add branch info if available
            (System.getenv("GITHUB_REF_NAME") ?: System.getenv("BRANCH_NAME"))?.let { branch ->
                tag(branch)
                value("Git branch", branch)
            }

            // Add build info
            val serverUrl = System.getenv("GITHUB_SERVER_URL")
            val repo = System.getenv("GITHUB_REPOSITORY")
            val runId = System.getenv("GITHUB_RUN_ID")
            if (serverUrl != null && repo != null && runId != null) {
                link("GitHub Actions Build", "$serverUrl/$repo/actions/runs/$runId")
            }
        }
    }
}

// Set project directories for all modules
// Platform layer
project(":platform-commons").projectDir = file("platform/commons")
project(":platform-observability").projectDir = file("platform/observability")
project(":platform-application-commons").projectDir = file("platform/application-commons")
project(":platform-domain-commons").projectDir = file("platform/domain-commons")
project(":platform-infrastructure").projectDir = file("platform/infrastructure")

// Interface layer
project(":interfaces-cli").projectDir = file("interfaces/cli")

// Contracts layer
project(":contracts-scope-management").projectDir = file("contracts/scope-management")
project(":contracts-user-preferences").projectDir = file("contracts/user-preferences")
project(":contracts-event-store").projectDir = file("contracts/event-store")
project(":contracts-device-synchronization").projectDir = file("contracts/device-synchronization")
project(":contracts-collaborative-versioning").projectDir = file("contracts/collaborative-versioning")
project(":contracts-agent-management").projectDir = file("contracts/agent-management")

// Bounded Context - Scope Management
project(":scope-management-domain").projectDir = file("contexts/scope-management/domain")
project(":scope-management-application").projectDir = file("contexts/scope-management/application")
project(":scope-management-infrastructure").projectDir = file("contexts/scope-management/infrastructure")

// Bounded Context - User Preferences
project(":user-preferences-domain").projectDir = file("contexts/user-preferences/domain")
project(":user-preferences-application").projectDir = file("contexts/user-preferences/application")
project(":user-preferences-infrastructure").projectDir = file("contexts/user-preferences/infrastructure")

// Bounded Context - Event Store
project(":event-store-domain").projectDir = file("contexts/event-store/domain")
project(":event-store-application").projectDir = file("contexts/event-store/application")
project(":event-store-infrastructure").projectDir = file("contexts/event-store/infrastructure")

// Bounded Context - Device Synchronization
project(":device-synchronization-domain").projectDir = file("contexts/device-synchronization/domain")
project(":device-synchronization-application").projectDir = file("contexts/device-synchronization/application")
project(":device-synchronization-infrastructure").projectDir = file("contexts/device-synchronization/infrastructure")

// Bounded Context - Collaborative Versioning
project(":collaborative-versioning-domain").projectDir = file("contexts/collaborative-versioning/domain")
project(":collaborative-versioning-application").projectDir = file("contexts/collaborative-versioning/application")
project(":collaborative-versioning-infrastructure").projectDir = file("contexts/collaborative-versioning/infrastructure")

// Bounded Context - Agent Management
project(":agent-management-domain").projectDir = file("contexts/agent-management/domain")
project(":agent-management-application").projectDir = file("contexts/agent-management/application")
project(":agent-management-infrastructure").projectDir = file("contexts/agent-management/infrastructure")

// Apps layer
project(":apps-scopes").projectDir = file("apps/scopes")
project(":apps-scopesd").projectDir = file("apps/scopesd")

// Quality
project(":quality-konsist").projectDir = file("quality/konsist")
