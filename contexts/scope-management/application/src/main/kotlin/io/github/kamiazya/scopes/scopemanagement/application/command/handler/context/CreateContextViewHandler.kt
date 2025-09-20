package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
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
class CreateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
) : CommandHandler<CreateContextViewCommand, ScopeContractError, ContextViewDto> {

    override suspend operator fun invoke(command: CreateContextViewCommand): Either<ScopeContractError, ContextViewDto> = transactionManager.inTransaction {
        either {
            // Validate and create value objects
            val key = ContextViewKey.create(command.key)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()
            val name = ContextViewName.create(command.name)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()
            val filter = ContextViewFilter.create(command.filter)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()

            // Check if a context with the same key already exists
            contextViewRepository.findByKey(key).fold(
                { error ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "context-view-repository",
                        ),
                    )
                },
                { existing ->
                    if (existing != null) {
                        raise(
                            ScopeContractError.BusinessError.DuplicateContextKey(
                                contextKey = key.value,
                                existingContextId = existing.id.value.toString(),
                            ),
                        )
                    }
                },
            )

            // Create the context view
            val contextView = ContextView.create(
                key = key,
                name = name,
                filter = filter,
                description = command.description,
                now = Clock.System.now(),
            ).mapLeft { applicationErrorMapper.mapDomainError(it) }.bind()

            // Save to repository
            val saved = contextViewRepository.save(contextView).fold(
                { _ ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "context-view-repository",
                        ),
                    )
                },
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
}
