package io.github.kamiazya.scopes.scopemanagement.application.query

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository

/**
 * Use case for listing all aspect definitions.
 */
class ListAspectDefinitionsUseCase(private val aspectDefinitionRepository: AspectDefinitionRepository) {
    suspend fun execute(): Either<ScopesError, List<AspectDefinition>> = either {
        aspectDefinitionRepository.findAll().fold(
            { error -> raise(ScopesError.SystemError("Failed to list aspect definitions: $error")) },
            { definitions -> definitions },
        )
    }
}
