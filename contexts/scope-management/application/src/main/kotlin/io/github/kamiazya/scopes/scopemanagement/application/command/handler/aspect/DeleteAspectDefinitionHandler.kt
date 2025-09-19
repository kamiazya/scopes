package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
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
    private val applicationErrorMapper: ApplicationErrorMapper,
) : CommandHandler<DeleteAspectDefinitionCommand, ScopeContractError, Unit> {

    override suspend operator fun invoke(command: DeleteAspectDefinitionCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(command.key)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()

            // Check if definition exists
            aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "aspect-definition-repository",
                        ),
                    )
                },
                { definition ->
                    definition ?: raise(
                        ScopeContractError.BusinessError.NotFound(
                            scopeId = command.key,
                        ),
                    )
                },
            )

            // Check if aspect is in use by any scopes
            aspectUsageValidationService.ensureNotInUse(aspectKey)
                .mapLeft { applicationErrorMapper.mapToContractError(it) }
                .bind()

            // Delete from repository
            aspectDefinitionRepository.deleteByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "aspect-definition-repository",
                        ),
                    )
                },
                { Unit },
            )
        }
    }
}
