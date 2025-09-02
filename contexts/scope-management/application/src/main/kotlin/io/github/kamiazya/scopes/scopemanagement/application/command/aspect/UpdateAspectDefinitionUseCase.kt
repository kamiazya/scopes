package io.github.kamiazya.scopes.scopemanagement.application.command.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Use case for updating an existing aspect definition.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionUseCase(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    UseCase<UpdateAspectDefinitionUseCase.Command, ScopesError, AspectDefinition> {

    data class Command(val key: String, val description: String? = null)

    override suspend operator fun invoke(input: Command): Either<ScopesError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(input.key).bind()

            // Find existing definition
            val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to retrieve aspect definition: $error")) },
                { definition ->
                    definition ?: raise(ScopesError.NotFound("Aspect definition with key '${input.key}' not found"))
                },
            )

            // Update only if description is provided and different
            if (input.description == null || input.description == existing.description) {
                return@either existing
            }

            val updated = existing.copy(description = input.description)

            // Save updated definition
            aspectDefinitionRepository.save(updated).fold(
                { error -> raise(ScopesError.SystemError("Failed to update aspect definition: $error")) },
                { saved -> saved },
            )
        }
    }
}
