package io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAspectDefinitions
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository

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
) : QueryHandler<ListAspectDefinitions, ScopeManagementApplicationError, List<AspectDefinitionDto>> {

    override suspend operator fun invoke(query: ListAspectDefinitions): Either<ScopeManagementApplicationError, List<AspectDefinitionDto>> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Listing all aspect definitions",
                mapOf<String, Any>(),
            )
            either {
                // Note: Current implementation returns all definitions
                // TODO: Implement pagination support in repository
                val definitions = aspectDefinitionRepository.findAll()
                    .mapLeft { _ ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "list-aspect-definitions",
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
