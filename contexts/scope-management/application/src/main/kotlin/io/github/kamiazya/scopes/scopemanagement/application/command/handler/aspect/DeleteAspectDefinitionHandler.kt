package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for deleting an aspect definition.
 * Validates that the aspect is not in use before allowing deletion.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 */
class DeleteAspectDefinitionHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val aspectUsageValidationService: AspectUsageValidationService,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<DeleteAspectDefinitionCommand, Unit>(transactionManager, logger) {

    override suspend fun executeCommand(command: DeleteAspectDefinitionCommand): Either<ScopeContractError, Unit> = either {
        logger.info(
            "Deleting aspect definition",
            mapOf("aspectKey" to command.key),
        )
        
        // Validate and create aspect key
        val aspectKey = AspectKey.create(command.key)
            .mapLeft { error ->
                logger.error(
                    "Invalid aspect key",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        // Check if definition exists
        val existing = aspectDefinitionRepository.findByKey(aspectKey)
            .mapLeft { error ->
                logger.error(
                    "Failed to find aspect definition",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()
        
        if (existing == null) {
            logger.error(
                "Aspect definition not found",
                mapOf("key" to command.key),
            )
            raise(
                ScopeContractError.BusinessError.NotFound(
                    scopeId = command.key,
                ),
            )
        }

        // Check if aspect is in use by any scopes
        aspectUsageValidationService.ensureNotInUse(aspectKey)
            .mapLeft { error ->
                logger.error(
                    "Aspect is in use",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapToContractError(error)
            }
            .bind()

        // Delete from repository
        aspectDefinitionRepository.deleteByKey(aspectKey)
            .mapLeft { error ->
                logger.error(
                    "Failed to delete aspect definition",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()
        
        logger.info(
            "Aspect definition deleted successfully",
            mapOf("aspectKey" to command.key),
        )
    }
}
