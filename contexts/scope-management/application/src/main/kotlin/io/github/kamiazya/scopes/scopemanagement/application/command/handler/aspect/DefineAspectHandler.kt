package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Handler for defining a new aspect.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 * Creates and persists an AspectDefinition with the specified type.
 */
class DefineAspectHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<DefineAspectCommand, AspectDefinition>(transactionManager, logger) {

    override suspend fun executeCommand(command: DefineAspectCommand): Either<ScopeContractError, AspectDefinition> = either {
        logger.info(
            "Defining new aspect",
            mapOf<String, Any>(
                "aspectKey" to command.key,
                "aspectType" to checkNotNull(command.type::class.simpleName) { "AspectType class name should not be null" },
                "description" to command.description,
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

        // Check if aspect already exists
        val existing = aspectDefinitionRepository.findByKey(aspectKey)
            .mapLeft { error ->
                logger.error(
                    "Failed to check existing aspect",
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

        if (existing != null) {
            logger.error(
                "Aspect already exists",
                mapOf<String, Any>("key" to command.key),
            )
            raise(
                ScopeContractError.BusinessError.DuplicateTitle(
                    title = command.key,
                    parentId = null,
                ),
            )
        }

        // Create aspect definition based on type
        val aspectDefinition = when (command.type) {
            is AspectType.Text -> AspectDefinition.createText(
                key = aspectKey,
                description = command.description,
            )
            is AspectType.Numeric -> AspectDefinition.createNumeric(
                key = aspectKey,
                description = command.description,
            )
            is AspectType.BooleanType -> AspectDefinition.createBoolean(
                key = aspectKey,
                description = command.description,
            )
            is AspectType.Ordered -> AspectDefinition.createOrdered(
                key = aspectKey,
                description = command.description,
                allowedValues = command.type.allowedValues,
            ).mapLeft { error ->
                logger.error(
                    "Failed to create ordered aspect",
                    mapOf<String, Any>(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }.bind()
            is AspectType.Duration -> AspectDefinition.createDuration(
                key = aspectKey,
                description = command.description,
            )
        }

        // Save to repository
        val saved = aspectDefinitionRepository.save(aspectDefinition)
            .mapLeft { error ->
                logger.error(
                    "Failed to save aspect definition",
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
            "Aspect definition created successfully",
            mapOf<String, Any>(
                "aspectKey" to saved.key.value,
                "aspectType" to checkNotNull(saved.type::class.simpleName) { "AspectType class name should not be null" },
            ),
        )

        saved
    }
}
