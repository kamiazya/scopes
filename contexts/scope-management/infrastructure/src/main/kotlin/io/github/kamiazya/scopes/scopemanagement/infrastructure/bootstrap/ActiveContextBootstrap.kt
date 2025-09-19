package io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap

import arrow.core.Either
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper
import io.github.kamiazya.scopes.platform.application.lifecycle.BootstrapError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightActiveContextRepository

/**
 * Bootstrapper for initializing the ActiveContext repository.
 *
 * This bootstrapper ensures that the active_context table is properly
 * initialized at application startup, replacing the previous pattern
 * of using runBlocking in module creation.
 */
class ActiveContextBootstrap(private val activeContextRepository: SqlDelightActiveContextRepository, private val logger: Logger) : ApplicationBootstrapper {
    override val name: String = "ActiveContextBootstrap"
    override val priority: Int = 90 // Lower priority than database initialization but before features

    override suspend fun initialize(): Either<BootstrapError, Unit> {
        logger.info("Initializing ActiveContext repository", mapOf("bootstrapper" to name))
        return activeContextRepository.initialize()
            .mapLeft { _ ->
                BootstrapError(
                    component = name,
                    message = "Failed to initialize ActiveContext repository",
                    cause = null,
                    isCritical = false,
                )
            }
            .onLeft { error ->
                logger.error(
                    "Failed to initialize ActiveContext repository",
                    mapOf("bootstrapper" to name, "error" to error.message),
                )
            }
            .onRight {
                logger.info("ActiveContext repository initialized successfully", mapOf("bootstrapper" to name))
            }
    }
}
