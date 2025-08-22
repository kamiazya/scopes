plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
    application
}

dependencies {
    // Apps layer
    implementation(project(":apps:daemon"))

    // Infrastructure implementations - Unified Scope Management
    implementation(project(":contexts:scope-management:infrastructure"))

    // Core libraries
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)

    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}

application {
    mainClass.set("io.github.kamiazya.scopes.boot.daemon.MainKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("scopesd")
            mainClass.set("io.github.kamiazya.scopes.boot.daemon.MainKt")
            useFatJar.set(true)

            buildArgs.addAll(
                listOf(
                    "--no-fallback",
                    "--initialize-at-build-time=org.slf4j",
                    "-H:+ReportExceptionStackTraces",
                ),
            )
        }
    }
}
