package io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAspectDefinitions
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository

/**
 * Handler for listing all aspect definitions.
 *
 * This handler retrieves all aspect definitions from the repository
 * and maps them to DTOs for external consumption.
 */
class ListAspectDefinitionsHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    QueryHandler<ListAspectDefinitions, ScopesError, List<AspectDefinitionDto>> {

    override suspend operator fun invoke(query: ListAspectDefinitions): Either<ScopesError, List<AspectDefinitionDto>> = transactionManager.inTransaction {
        either {
            // Note: Current implementation returns all definitions
            // TODO: Implement pagination support in repository
            val definitions = aspectDefinitionRepository.findAll()
                .mapLeft { ScopesError.SystemError("Failed to list aspect definitions: $it") }
                .bind()

            // Map to DTOs
            definitions.map { definition ->
                AspectDefinitionDto(
                    key = definition.key.value,
                    type = definition.type.toString(),
                    description = definition.description,
                    allowMultiple = definition.allowMultiple,
                )
            }
        }
    }
}
