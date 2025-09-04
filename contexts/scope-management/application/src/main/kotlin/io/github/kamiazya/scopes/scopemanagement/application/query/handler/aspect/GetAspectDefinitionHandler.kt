package io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.dto.aspect.AspectDefinitionDto
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetAspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for retrieving an aspect definition by key.
 *
 * This handler validates the key, retrieves the aspect definition from the repository,
 * and maps it to a DTO for external consumption.
 */
class GetAspectDefinitionHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    QueryHandler<GetAspectDefinition, ScopesError, AspectDefinitionDto?> {

    override suspend operator fun invoke(query: GetAspectDefinition): Either<ScopesError, AspectDefinitionDto?> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(query.key).bind()

            // Find by key
            val aspectDefinition = aspectDefinitionRepository.findByKey(aspectKey)
                .mapLeft { ScopesError.SystemError("Failed to find aspect definition: $it") }
                .bind()

            // Map to DTO if found
            aspectDefinition?.let {
                AspectDefinitionDto(
                    key = it.key.value,
                    type = it.type.toString(),
                    description = it.description,
                    allowMultiple = it.allowMultiple,
                )
            }
        }
    }
}
