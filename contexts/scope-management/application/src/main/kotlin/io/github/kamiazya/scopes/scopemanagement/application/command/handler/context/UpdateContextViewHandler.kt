package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for updating an existing context view.
 *
 * This handler validates the input, retrieves the existing context view,
 * applies the updates, and persists the changes.
 */
class UpdateContextViewHandler(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) :
    CommandHandler<UpdateContextViewCommand, ScopesError, ContextViewDto> {

    override suspend operator fun invoke(command: UpdateContextViewCommand): Either<ScopesError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val key = ContextViewKey.create(command.key).mapLeft { it as ScopesError }.bind()

            // Retrieve existing context view
            val existingContextView = contextViewRepository.findByKey(key).mapLeft { it as ScopesError }.bind()
                ?: raise(
                    ScopesError.NotFound(
                        entityType = "ContextView",
                        identifier = command.key,
                        identifierType = "key",
                        occurredAt = Clock.System.now(),
                    ),
                )

            // Prepare updated values
            val updatedName = command.name?.let { newName ->
                ContextViewName.create(newName).mapLeft { it as ScopesError }.bind()
            } ?: existingContextView.name

            val updatedFilter = command.filter?.let { newFilter ->
                ContextViewFilter.create(newFilter).mapLeft { it as ScopesError }.bind()
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
                    updatedContextView.updateDescription("", Clock.System.now()).mapLeft { it as ScopesError }.bind()
                } else {
                    updatedContextView.updateDescription(command.description, Clock.System.now()).mapLeft { it as ScopesError }.bind()
                }
            }

            // Save to repository (using save method for updates)
            val saved = contextViewRepository.save(updatedContextView).mapLeft { it as ScopesError }.bind()

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
