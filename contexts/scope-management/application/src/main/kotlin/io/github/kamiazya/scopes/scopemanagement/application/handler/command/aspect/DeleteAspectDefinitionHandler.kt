package io.github.kamiazya.scopes.scopemanagement.application.handler.command.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for deleting an aspect definition.
 * Validates that the aspect is not in use before allowing deletion.
 */
class DeleteAspectDefinitionHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val aspectUsageValidationService: AspectUsageValidationService,
    private val transactionManager: TransactionManager,
) : CommandHandler<DeleteAspectDefinitionCommand, ScopesError, Unit> {

    override suspend operator fun invoke(command: DeleteAspectDefinitionCommand): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(command.key).bind()

            // Check if definition exists
            val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "retrieve-aspect-definition", "key" to command.key),
                        ),
                    )
                },
                { definition ->
                    definition ?: raise(
                        ScopesError.NotFound(
                            entityType = "AspectDefinition",
                            identifier = command.key,
                            identifierType = "key",
                        ),
                    )
                },
            )

            // Check if aspect is in use by any scopes
            aspectUsageValidationService.ensureNotInUse(aspectKey).bind()

            // Delete from repository
            aspectDefinitionRepository.deleteByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "delete-aspect-definition", "key" to command.key),
                        ),
                    )
                },
                { Unit },
            )
        }
    }
}
