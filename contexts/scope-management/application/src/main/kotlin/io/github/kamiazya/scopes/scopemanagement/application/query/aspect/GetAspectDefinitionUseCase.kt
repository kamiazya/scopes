package io.github.kamiazya.scopes.scopemanagement.application.query.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Use case for retrieving an aspect definition by key.
 */
class GetAspectDefinitionUseCase(private val aspectDefinitionRepository: AspectDefinitionRepository) :
    UseCase<GetAspectDefinitionUseCase.Query, ScopesError, AspectDefinition?> {

    data class Query(val key: String)

    override suspend operator fun invoke(input: Query): Either<ScopesError, AspectDefinition?> = either {
        // Validate and create aspect key
        val aspectKey = AspectKey.create(input.key).bind()

        // Find by key
        aspectDefinitionRepository.findByKey(aspectKey).fold(
            { error -> raise(ScopesError.SystemError("Failed to retrieve aspect definition: $error")) },
            { definition -> definition },
        )
    }
}
