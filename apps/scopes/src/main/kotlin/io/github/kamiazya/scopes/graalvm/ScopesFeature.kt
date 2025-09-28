package io.github.kamiazya.scopes.graalvm

import org.graalvm.nativeimage.hosted.Feature
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization

/**
 * GraalVM Native Image Feature for Scopes CLI
 *
 * This feature configures runtime initialization for classes that
 * cannot be initialized at build time due to native code dependencies
 * or static state that depends on the runtime environment.
 */
class ScopesFeature : Feature {
    override fun beforeAnalysis(access: Feature.BeforeAnalysisAccess) {
        // SQLite JDBC native library loading
        RuntimeClassInitialization.initializeAtRunTime("org.sqlite")

        // Note: Logback, SLF4J, and gRPC Netty are now initialized at build time
        // via native-image.properties to avoid initialization conflicts

        // Coroutines exception handling
        RuntimeClassInitialization.initializeAtRunTime("kotlinx.coroutines.CoroutineExceptionHandlerImplKt")

        // JNA for terminal handling
        RuntimeClassInitialization.initializeAtRunTime("com.sun.jna")

        // Kotlin UUID secure random
        RuntimeClassInitialization.initializeAtRunTime("kotlin.uuid.SecureRandomHolder")
    }
}
