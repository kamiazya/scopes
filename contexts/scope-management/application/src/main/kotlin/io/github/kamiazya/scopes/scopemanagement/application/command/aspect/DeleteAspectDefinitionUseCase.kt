package io.github.kamiazya.scopes.scopemanagement.application.command.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Use case for deleting an aspect definition.
 * Validates that the aspect is not in use before allowing deletion.
 */
class DeleteAspectDefinitionUseCase(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val aspectUsageValidationService: AspectUsageValidationService,
    private val transactionManager: TransactionManager,
) : UseCase<DeleteAspectDefinitionUseCase.Command, ScopesError, Unit> {

    data class Command(val key: String)

    override suspend operator fun invoke(input: Command): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(input.key).bind()

            // Check if definition exists
            val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to retrieve aspect definition: $error")) },
                { definition ->
                    definition ?: raise(ScopesError.NotFound("Aspect definition with key '${input.key}' not found"))
                },
            )

            // Check if aspect is in use by any scopes
            aspectUsageValidationService.ensureNotInUse(aspectKey).bind()

            // Delete from repository
            aspectDefinitionRepository.deleteByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to delete aspect definition: $error")) },
                { Unit },
            )
        }
    }
}
