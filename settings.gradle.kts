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
