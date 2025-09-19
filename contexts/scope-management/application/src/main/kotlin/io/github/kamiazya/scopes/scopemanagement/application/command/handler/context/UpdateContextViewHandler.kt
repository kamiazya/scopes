package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for updating an existing context view.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 *
 * This handler validates the input, retrieves the existing context view,
 * applies the updates, and persists the changes.
 */
class UpdateContextViewHandler(private val contextViewRepository: ContextViewRepository, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<UpdateContextViewCommand, ContextViewDto>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: UpdateContextViewCommand): Either<ScopeManagementApplicationError, ContextViewDto> = either {
        // Validate and create key value object
        val key = ContextViewKey.create(command.key)
            .mapLeft { errorMappingService.mapDomainError(it, "update-context-key") }
            .bind()

        // Retrieve existing context view
        val existingContextView = contextViewRepository.findByKey(key).fold(
            { _ -> raise(ScopeManagementApplicationError.PersistenceError.StorageUnavailable("find-context-view")) },
            { it },
        ) ?: raise(
            ScopeManagementApplicationError.PersistenceError.NotFound(
                entityType = "ContextView",
                entityId = command.key,
            ),
        )

        // Prepare updated values
        val updatedName = command.name?.let { newName ->
            ContextViewName.create(newName)
                .mapLeft { errorMappingService.mapDomainError(it, "update-context-name") }
                .bind()
        } ?: existingContextView.name

        val updatedFilter = command.filter?.let { newFilter ->
            ContextViewFilter.create(newFilter)
                .mapLeft { errorMappingService.mapDomainError(it, "update-context-filter") }
                .bind()
        } ?: existingContextView.filter

        // Handle description update logic
        val shouldUpdateDescription = command.description != null

        // Update the context view using the entity's update methods
        var updatedContextView = existingContextView

        if (command.name != null) {
            updatedContextView = updatedContextView.updateName(updatedName, Clock.System.now())
        }

        if (command.filter != null) {
            updatedContextView = updatedContextView.updateFilter(updatedFilter, Clock.System.now())
        }

        if (shouldUpdateDescription) {
            updatedContextView = if (command.description.isNullOrEmpty()) {
                // Clear description by passing empty string
                updatedContextView.updateDescription("", Clock.System.now())
                    .mapLeft { errorMappingService.mapDomainError(it, "update-context-description") }
                    .bind()
            } else {
                updatedContextView.updateDescription(command.description, Clock.System.now())
                    .mapLeft { errorMappingService.mapDomainError(it, "update-context-description") }
                    .bind()
            }
        }

        // Save to repository (using save method for updates)
        val saved = contextViewRepository.save(updatedContextView).fold(
            { _ -> raise(ScopeManagementApplicationError.PersistenceError.StorageUnavailable("save-context-view")) },
            { it },
        )

        // Map to DTO
        ContextViewDto(
            id = saved.id.value.toString(),
            key = saved.key.value,
            name = saved.name.value,
            filter = saved.filter.expression,
            description = saved.description?.value,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
        )
    }
}
