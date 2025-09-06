package io.github.kamiazya.scopes.platform.application.lifecycle

import arrow.core.Either

/**
 * Represents a component that requires initialization during application startup.
 *
 * Components implementing this interface will be automatically initialized
 * during the application bootstrap phase.
 *
 * Key characteristics:
 * - Initialization should be idempotent (safe to call multiple times)
 * - Initialization order can be controlled via priority
 * - Failures are reported but don't stop other bootstrappers
 *
 * Example usage:
 * ```
 * class DatabaseMigrationBootstrapper : ApplicationBootstrapper {
 *     override val name = "DatabaseMigration"
 *     override val priority = 100 // Higher priority runs first
 *
 *     override suspend fun initialize(): Either<BootstrapError, Unit> {
 *         return runMigrations()
 *     }
 * }
 * ```
 */
interface ApplicationBootstrapper {
    /**
     * Human-readable name for this bootstrapper.
     * Used for logging and error reporting.
     */
    val name: String

    /**
     * Priority for initialization order.
     * Higher values are initialized first.
     * Default is 0.
     *
     * Common priority ranges:
     * - 1000+: Critical infrastructure (databases, connections)
     * - 500-999: Core services
     * - 100-499: Feature initialization
     * - 0-99: Optional features
     */
    val priority: Int get() = 0

    /**
     * Performs the initialization logic.
     *
     * This method should:
     * - Be idempotent (safe to call multiple times)
     * - Not throw exceptions (use Either.Left instead)
     * - Complete in a reasonable time
     * - Not block on user input
     *
     * @return Either.Right(Unit) on success, Either.Left(BootstrapError) on failure
     */
    suspend fun initialize(): Either<BootstrapError, Unit>
}

/**
 * Error type for bootstrap failures.
 */
data class BootstrapError(val component: String, val message: String, val cause: Throwable? = null, val isCritical: Boolean = false)

/**
 * Manages the application lifecycle and bootstrap process.
 *
 * This service coordinates the initialization of all registered
 * ApplicationBootstrapper components during application startup.
 *
 * Key features:
 * - Ordered initialization based on priority
 * - Parallel initialization within same priority
 * - Graceful error handling
 * - Idempotent operation
 */
interface ApplicationLifecycleManager {
    /**
     * Initializes all registered bootstrappers.
     *
     * Bootstrappers are initialized in priority order (highest first).
     * Within the same priority level, initialization may happen in parallel.
     *
     * @return List of any bootstrap errors that occurred (empty on complete success)
     */
    suspend fun initialize(): List<BootstrapError>

    /**
     * Checks if the application has been initialized.
     */
    fun isInitialized(): Boolean

    /**
     * Shuts down the application gracefully.
     * This is called when the application is closing.
     */
    suspend fun shutdown()
}
