package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for updating an existing aspect definition.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<UpdateAspectDefinitionCommand, AspectDefinition>(transactionManager, logger) {

    override suspend fun executeCommand(command: UpdateAspectDefinitionCommand): Either<ScopeContractError, AspectDefinition> = either {
        logger.info(
            "Updating aspect definition",
            mapOf<String, Any>(
                "aspectKey" to command.key,
                "newDescription" to (command.description ?: "none"),
            ),
        )

        // Validate and create aspect key
        val aspectKey = AspectKey.create(command.key)
            .mapLeft { error ->
                logger.error(
                    "Invalid aspect key",
                    mapOf<String, Any>(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        // Find existing definition
        val existing = aspectDefinitionRepository.findByKey(aspectKey)
            .mapLeft { error ->
                logger.error(
                    "Failed to find aspect definition",
                    mapOf<String, Any>(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                // Repository errors should be mapped to ServiceUnavailable
                ScopeContractError.SystemError.ServiceUnavailable(
                    service = "aspect-definition-repository",
                )
            }
            .bind()

        if (existing == null) {
            logger.error(
                "Aspect definition not found",
                mapOf<String, Any>("key" to command.key),
            )
            raise(
                ScopeContractError.BusinessError.NotFound(
                    scopeId = command.key,
                ),
            )
        }

        // Update only if description is provided and different
        if (command.description == null || command.description == existing.description) {
            logger.info(
                "No changes to aspect definition",
                mapOf<String, Any>("key" to command.key),
            )
            return@either existing
        }

        val updated = existing.copy(description = command.description)

        // Save updated definition
        val saved = aspectDefinitionRepository.save(updated)
            .mapLeft { error ->
                logger.error(
                    "Failed to save updated aspect definition",
                    mapOf<String, Any>(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                // Repository errors should be mapped to ServiceUnavailable
                ScopeContractError.SystemError.ServiceUnavailable(
                    service = "aspect-definition-repository",
                )
            }
            .bind()

        logger.info(
            "Aspect definition updated successfully",
            mapOf<String, Any>(
                "aspectKey" to saved.key.value,
                "newDescription" to (saved.description ?: "none"),
            ),
        )

        saved
    }
}
