package io.github.kamiazya.scopes.scopemanagement.application.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Use case for updating an existing aspect definition.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionUseCase(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) {
    suspend fun execute(key: String, description: String? = null): Either<ScopesError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(key).bind()

            // Find existing definition
            val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to retrieve aspect definition: $error")) },
                { definition ->
                    definition ?: raise(ScopesError.NotFound("Aspect definition with key '$key' not found"))
                },
            )

            // Update only the description for now
            // Changing type would require data migration
            val updated = if (description != null && description != existing.description) {
                existing.copy(description = description)
            } else {
                existing
            }

            // Save updated definition
            aspectDefinitionRepository.save(updated).fold(
                { error -> raise(ScopesError.SystemError("Failed to update aspect definition: $error")) },
                { saved -> saved },
            )
        }
    }
}
