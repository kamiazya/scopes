package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for updating an existing aspect definition.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<UpdateAspectDefinitionCommand, AspectDefinition>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: UpdateAspectDefinitionCommand): Either<ScopeManagementApplicationError, AspectDefinition> = either {
        fun raiseStorageError(operation: String): Nothing = raise(ScopeManagementApplicationError.PersistenceError.StorageUnavailable(operation))
        // Validate and create aspect key
        val aspectKey = AspectKey.create(command.key)
            .mapLeft { errorMappingService.mapDomainError(it, "update-aspect-key") }
            .bind()

        // Find existing definition
        val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
            { raiseStorageError("find-aspect-definition") },
            { it },
        ) ?: raise(
            ScopeManagementApplicationError.PersistenceError.NotFound(
                entityType = "AspectDefinition",
                entityId = command.key,
            ),
        )

        // Update only if description is provided and different
        if (command.description == null || command.description == existing.description) {
            return@either existing
        }

        val updated = existing.copy(description = command.description)

        // Save updated definition
        aspectDefinitionRepository.save(updated).fold(
            { raiseStorageError("save-aspect-definition") },
            { it },
        )
    }
}
