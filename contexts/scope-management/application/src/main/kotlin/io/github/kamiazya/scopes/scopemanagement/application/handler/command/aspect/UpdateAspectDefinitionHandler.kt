package io.github.kamiazya.scopes.scopemanagement.application.handler.command.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import kotlinx.datetime.Clock

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
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "retrieve-aspect-definition", "key" to command.key),
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
                { definition ->
                    definition ?: raise(
                        ScopesError.NotFound(
                            entityType = "AspectDefinition",
                            identifier = command.key,
                            identifierType = "key",
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
            )

            // Update only if description is provided and different
            if (command.description == null || command.description == existing.description) {
                return@either existing
            }

            val updated = existing.copy(description = command.description)

            // Save updated definition
            aspectDefinitionRepository.save(updated).fold(
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "update-aspect-definition", "key" to command.key),
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
                { saved -> saved },
            )
        }
    }
}
