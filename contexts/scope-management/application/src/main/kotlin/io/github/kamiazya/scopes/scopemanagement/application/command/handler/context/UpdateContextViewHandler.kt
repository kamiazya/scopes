package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
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
class UpdateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
) : CommandHandler<UpdateContextViewCommand, ScopeContractError, ContextViewDto> {

    override suspend operator fun invoke(command: UpdateContextViewCommand): Either<ScopeContractError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create key value object
            val key = ContextViewKey.create(command.key)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()

            // Retrieve existing context view
            val existingContextView = contextViewRepository.findByKey(key).fold(
                { _ ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "context-view-repository",
                        ),
                    )
                },
                { context ->
                    context ?: raise(
                        ScopeContractError.BusinessError.ContextNotFound(
                            contextKey = command.key,
                        ),
                    )
                },
            )

            // Prepare updated values
            val updatedName = command.name?.let { newName ->
                ContextViewName.create(newName)
                    .mapLeft { applicationErrorMapper.mapDomainError(it) }
                    .bind()
            } ?: existingContextView.name

            val updatedFilter = command.filter?.let { newFilter ->
                ContextViewFilter.create(newFilter)
                    .mapLeft { applicationErrorMapper.mapDomainError(it) }
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
                        .mapLeft { applicationErrorMapper.mapDomainError(it) }
                        .bind()
                } else {
                    updatedContextView.updateDescription(command.description, Clock.System.now())
                        .mapLeft { applicationErrorMapper.mapDomainError(it) }
                        .bind()
                }
            }

            // Save to repository (using save method for updates)
            val saved = contextViewRepository.save(updatedContextView).fold(
                { _ ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "context-view-repository",
                        ),
                    )
                },
                { saved -> saved },
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
}
