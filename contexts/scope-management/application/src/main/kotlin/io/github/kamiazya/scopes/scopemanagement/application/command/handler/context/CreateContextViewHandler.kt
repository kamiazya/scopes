package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError as DomainContextError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for creating a new context view.
 *
 * This handler validates the input, ensures the key is unique,
 * validates the filter syntax, and persists the new context view.
 */
class CreateContextViewHandler(private val contextViewRepository: ContextViewRepository, private val transactionManager: TransactionManager) :
    CommandHandler<CreateContextViewCommand, ScopesError, ContextViewDto> {

    override suspend operator fun invoke(command: CreateContextViewCommand): Either<ScopesError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create value objects
            val key = ContextViewKey.create(command.key).bind()
            val name = ContextViewName.create(command.name).bind()
            val filter = ContextViewFilter.create(command.filter).bind()

            // Create the context view
            val contextView = ContextView.create(
                key = key,
                name = name,
                filter = filter,
                description = command.description,
                now = Clock.System.now(),
            ).bind()

            // Save to repository
            val saved = contextViewRepository.save(contextView).bind()

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
