package io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAspectDefinitions
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import kotlinx.datetime.Clock

/**
 * Handler for listing all aspect definitions.
 *
 * This handler retrieves all aspect definitions from the repository
 * and maps them to DTOs for external consumption.
 */
class ListAspectDefinitionsHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : QueryHandler<ListAspectDefinitions, ScopesError, List<AspectDefinitionDto>> {

    override suspend operator fun invoke(query: ListAspectDefinitions): Either<ScopesError, List<AspectDefinitionDto>> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Listing all aspect definitions",
                mapOf<String, Any>(),
            )
            either {
                // Note: Current implementation returns all definitions
                // TODO: Implement pagination support in repository
                val definitions = aspectDefinitionRepository.findAll()
                    .mapLeft { error ->
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "list-aspect-definitions"),
                            occurredAt = Clock.System.now(),
                        )
                    }
                    .bind()

                // Map to DTOs
                val result = definitions.map { definition ->
                    AspectDefinitionDto(
                        key = definition.key.value,
                        type = definition.type.toString(),
                        description = definition.description,
                        allowMultiple = definition.allowMultiple,
                    )
                }

                logger.info(
                    "Successfully listed aspect definitions",
                    mapOf(
                        "count" to result.size,
                    ),
                )

                result
            }
        }.onLeft { error ->
            logger.error(
                "Failed to list aspect definitions",
                mapOf(
                    "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                    "message" to error.toString(),
                ),
            )
        }
}
