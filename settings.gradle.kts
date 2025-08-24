pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradle.develocity") version "3.19.2"
}

rootProject.name = "scopes"

include(
    // Platform layer
    ":platform-commons",
    ":platform-observability",
    ":platform-application-commons",
    ":platform-domain-commons",
    // Interface layer
    ":interfaces-cli",
    // Contracts layer
    ":contracts-scope-management",
    ":contracts-user-preferences",
    // Bounded Context - Unified Scope Management
    ":scope-management-domain",
    ":scope-management-application",
    ":scope-management-infrastructure",
    // Bounded Context - User Preferences
    ":user-preferences-domain",
    ":user-preferences-application",
    ":user-preferences-infrastructure",
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

// Interface layer
project(":interfaces-cli").projectDir = file("interfaces/cli")

// Contracts layer
project(":contracts-scope-management").projectDir = file("contracts/scope-management")
project(":contracts-user-preferences").projectDir = file("contracts/user-preferences")

// Bounded Context - Scope Management
project(":scope-management-domain").projectDir = file("contexts/scope-management/domain")
project(":scope-management-application").projectDir = file("contexts/scope-management/application")
project(":scope-management-infrastructure").projectDir = file("contexts/scope-management/infrastructure")

// Bounded Context - User Preferences
project(":user-preferences-domain").projectDir = file("contexts/user-preferences/domain")
project(":user-preferences-application").projectDir = file("contexts/user-preferences/application")
project(":user-preferences-infrastructure").projectDir = file("contexts/user-preferences/infrastructure")

// Apps layer
project(":apps-scopes").projectDir = file("apps/scopes")
project(":apps-scopesd").projectDir = file("apps/scopesd")

// Quality
project(":quality-konsist").projectDir = file("quality/konsist")
