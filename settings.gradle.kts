rootProject.name = "scopes"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

include(
    ":domain",
    ":application",
    ":infrastructure",
    ":presentation-cli",
    ":konsist-test",
)
