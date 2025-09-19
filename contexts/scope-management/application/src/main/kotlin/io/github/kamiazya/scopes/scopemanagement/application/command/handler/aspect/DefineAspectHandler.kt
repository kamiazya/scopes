package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Handler for defining a new aspect.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 * Creates and persists an AspectDefinition with the specified type.
 */
class DefineAspectHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<DefineAspectCommand, AspectDefinition>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: DefineAspectCommand): Either<ScopeManagementApplicationError, AspectDefinition> = either {
        fun raiseStorageError(operation: String): Nothing = raise(ScopeManagementApplicationError.PersistenceError.StorageUnavailable(operation))
        // Validate and create aspect key
        val aspectKey = AspectKey.create(command.key)
            .mapLeft { errorMappingService.mapDomainError(it, "define-aspect-key") }
            .bind()

        // Check if aspect already exists
        val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
            { null }, // On error, assume not found
            { it }, // On success, return the result
        )

        if (existing != null) {
            raise(
                CrossAggregateValidationError.InvariantViolation(
                    invariantName = "aspect-key-uniqueness",
                    aggregateIds = listOf(command.key),
                    violationDescription = "Aspect definition with key '${command.key}' already exists",
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
            ).mapLeft { errorMappingService.mapDomainError(it, "define-aspect-ordered") }.bind()
            is AspectType.Duration -> AspectDefinition.createDuration(
                key = aspectKey,
                description = command.description,
            )
        }

        // Save to repository
        aspectDefinitionRepository.save(aspectDefinition).fold(
            { raiseStorageError("save-aspect-definition") },
            { it },
        )
    }
}
