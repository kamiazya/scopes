package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for updating an existing aspect definition.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    CommandHandler<UpdateAspectDefinitionCommand, ScopesError, AspectDefinition> {

    override suspend operator fun invoke(command: UpdateAspectDefinitionCommand): Either<ScopesError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(command.key).bind()

            // Find existing definition
            val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to retrieve aspect definition: $error")) },
                { definition ->
                    definition ?: raise(ScopesError.NotFound("Aspect definition with key '${command.key}' not found"))
                },
            )

            // Update only if description is provided and different
            if (command.description == null || command.description == existing.description) {
                return@either existing
            }

            val updated = existing.copy(description = command.description)

            // Save updated definition
            aspectDefinitionRepository.save(updated).fold(
                { error -> raise(ScopesError.SystemError("Failed to update aspect definition: $error")) },
                { saved -> saved },
            )
        }
    }
}
