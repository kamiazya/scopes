package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for creating a new context view.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 *
 * This handler validates the input, ensures the key is unique,
 * validates the filter syntax, and persists the new context view.
 */
class CreateContextViewHandler(private val contextViewRepository: ContextViewRepository, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<CreateContextViewCommand, ContextViewDto>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: CreateContextViewCommand): Either<ScopeManagementApplicationError, ContextViewDto> = either {
        fun raiseStorageError(operation: String): Nothing = raise(ScopeManagementApplicationError.PersistenceError.StorageUnavailable(operation))
        // Validate and create value objects
        val key = ContextViewKey.create(command.key)
            .mapLeft { errorMappingService.mapDomainError(it, "create-context-key") }
            .bind()
        val name = ContextViewName.create(command.name)
            .mapLeft { errorMappingService.mapDomainError(it, "create-context-name") }
            .bind()
        val filter = ContextViewFilter.create(command.filter)
            .mapLeft { errorMappingService.mapDomainError(it, "create-context-filter") }
            .bind()

        // Create the context view
        val contextView = ContextView.create(
            key = key,
            name = name,
            filter = filter,
            description = command.description,
            now = Clock.System.now(),
        ).mapLeft { errorMappingService.mapDomainError(it, "create-context-entity") }.bind()

        // Save to repository
        val saved = contextViewRepository.save(contextView).fold(
            { _ -> raiseStorageError("save-context-view") },
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
