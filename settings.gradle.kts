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
    ":domain",
    ":application",
    ":infrastructure",
    ":presentation-cli",
    ":konsist-test",
)

// Configure Gradle Build Scan
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"

        // Publish build scans for main branches and CI builds
        publishing.onlyIf {
            !gradle.startParameter.isOffline
        }

        // Capture build insights
        capture {
            buildLogging = true
            testLogging = true
        }

        // Add environment info
        tag(if (System.getenv("CI") != null) "CI" else "LOCAL")

        // Add branch info if available
        val branch = System.getenv("GITHUB_REF_NAME")
            ?: System.getenv("BRANCH_NAME")
        if (branch != null) {
            tag(branch)
            value("Git branch", branch)
        }

        // Add build info
        val buildUrl = System.getenv("GITHUB_SERVER_URL")?.let { serverUrl ->
            val repo = System.getenv("GITHUB_REPOSITORY")
            val runId = System.getenv("GITHUB_RUN_ID")
            if (repo != null && runId != null) "$serverUrl/$repo/actions/runs/$runId" else null
        }
        if (buildUrl != null) {
            link("GitHub Actions Build", buildUrl)
        }
    }
}
