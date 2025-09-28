package io.github.kamiazya.scopes.platform.infrastructure.lifecycle

import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationLifecycleManager
import io.github.kamiazya.scopes.platform.application.lifecycle.BootstrapError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Default implementation of ApplicationLifecycleManager.
 *
 * This implementation:
 * - Discovers all ApplicationBootstrapper instances via Koin DI
 * - Initializes them in priority order
 * - Runs bootstrappers with same priority in parallel
 * - Handles errors gracefully without stopping other bootstrappers
 * - Ensures thread-safe, idempotent operation
 */
class DefaultApplicationLifecycleManager(private val logger: Logger) :
    ApplicationLifecycleManager,
    KoinComponent {

    private val mutex = Mutex()
    private var initialized = false
    private val errors = mutableListOf<BootstrapError>()

    // Lazy injection of all bootstrappers
    private val bootstrappers: List<ApplicationBootstrapper> by inject()

    override suspend fun initialize(): List<BootstrapError> = mutex.withLock {
        if (initialized) {
            logger.debug("Application already initialized, returning cached errors")
            return errors.toList()
        }

        logger.info("Starting application initialization with ${bootstrappers.size} bootstrappers")

        // Group bootstrappers by priority
        val groupedByPriority = bootstrappers.groupBy { it.priority }
            .toSortedMap(compareByDescending { it })

        // Initialize each priority group
        coroutineScope {
            for ((priority, group) in groupedByPriority) {
                logger.debug("Initializing priority group $priority with ${group.size} bootstrappers")

                // Run all bootstrappers in this priority group in parallel
                val results = group.map { bootstrapper ->
                    async {
                        logger.debug("Initializing ${bootstrapper.name}")
                        try {
                            bootstrapper.initialize().fold(
                                ifLeft = { error ->
                                    logger.error("Failed to initialize ${bootstrapper.name}: ${error.message}", throwable = error.cause)
                                    error
                                },
                                ifRight = {
                                    logger.info("Successfully initialized ${bootstrapper.name}")
                                    null
                                },
                            )
                        } catch (e: Exception) {
                            // Catch any unexpected exceptions
                            val error = BootstrapError(
                                component = bootstrapper.name,
                                message = "Unexpected error during initialization",
                                cause = e,
                                isCritical = false,
                            )
                            logger.error("Unexpected error initializing ${bootstrapper.name}", throwable = e)
                            error
                        }
                    }
                }.awaitAll()

                // Collect errors from this priority group
                results.filterNotNull().forEach { error ->
                    errors.add(error)
                    if (error.isCritical) {
                        logger.error("Critical error during initialization, stopping further initialization")
                        initialized = true
                        return@coroutineScope errors.toList()
                    }
                }
            }
        }

        initialized = true
        logger.info("Application initialization completed with ${errors.size} errors")

        return errors.toList()
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun shutdown() {
        mutex.withLock {
            logger.info("Application shutdown initiated")
            // Future: Add shutdown logic for components that need cleanup
            initialized = false
            errors.clear()
        }
    }
}
