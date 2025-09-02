package io.github.kamiazya.scopes.scopemanagement.application.command.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError

/**
 * Use case for updating an existing context view.
 *
 * This use case validates the input, retrieves the existing context view,
 * applies the updates, and persists the changes.
 */
class UpdateContextViewUseCase(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) {
    suspend fun execute(command: UpdateContextViewCommand): Either<ApplicationError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val key = ContextViewKey.create(command.key)
                .mapLeft { (it as DomainContextError).toApplicationError() }
                .bind()

            // Retrieve existing context view
            val existingContextView = contextViewRepository.findByKey(key)
                .mapLeft { error ->
                    when (error) {
                        is DomainContextError -> error.toApplicationError()
                        else -> PersistenceError.StorageUnavailable(
                            operation = "find-context-view-by-key",
                            cause = error.toString(),
                        )
                    }
                }
                .bind()

            if (existingContextView == null) {
                raise(
                    PersistenceError.NotFound(
                        entityType = "ContextView",
                        entityId = command.key,
                    ),
                )
            }

            // Prepare updated values
            val updatedName = command.name?.let { newName ->
                ContextViewName.create(newName)
                    .mapLeft { (it as DomainContextError).toApplicationError() }
                    .bind()
            } ?: existingContextView.name

            val updatedFilter = command.filter?.let { newFilter ->
                ContextViewFilter.create(newFilter)
                    .mapLeft { (it as DomainContextError).toApplicationError() }
                    .bind()
            } ?: existingContextView.filter

            // Handle description update logic
            val shouldUpdateDescription = command.description != null

            // Update the context view using the entity's update methods
            var updatedContextView = existingContextView

            if (command.name != null) {
                updatedContextView = updatedContextView.updateName(updatedName)
            }

            if (command.filter != null) {
                updatedContextView = updatedContextView.updateFilter(updatedFilter)
            }

            if (shouldUpdateDescription) {
                updatedContextView = if (command.description.isNullOrEmpty()) {
                    // Clear description by passing empty string
                    updatedContextView.updateDescription("")
                        .mapLeft { (it as DomainContextError).toApplicationError() }
                        .bind()
                } else {
                    updatedContextView.updateDescription(command.description)
                        .mapLeft { (it as DomainContextError).toApplicationError() }
                        .bind()
                }
            }

            // Save to repository (using save method for updates)
            val saved = contextViewRepository.save(updatedContextView)
                .mapLeft { error ->
                    when (error) {
                        is DomainContextError -> error.toApplicationError()
                        else -> PersistenceError.StorageUnavailable(
                            operation = "save-context-view",
                            cause = error.toString(),
                        )
                    }
                }
                .bind()

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
}
