package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewInfo
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.command.CreateContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextDescription

/**
 * Handler for creating a new context view.
 * 
 * This handler:
 * 1. Validates the context name and filter expression
 * 2. Creates a new context view entity
 * 3. Persists it to the repository
 * 4. Returns the created context view information
 */
class CreateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager
) : CommandHandler<CreateContextView, ContextViewInfo> {

    override suspend fun invoke(command: CreateContextView): Either<ApplicationError, ContextViewInfo> {
        return transactionManager.inTransaction {
            // Validate and create context name
            val contextName = ContextName.create(command.name).fold(
                ifLeft = { errorMsg -> 
                    // Convert string error to ApplicationError
                    return@inTransaction ApplicationError.ContextError.NamingInvalidFormat(
                        attemptedName = command.name
                    ).left()
                },
                ifRight = { it }
            )

            // Validate and create filter
            val contextFilter = ContextFilter.create(command.filterExpression).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { it }
            )

            // Check if a context with the same name already exists
            contextViewRepository.findByName(contextName).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { existingContext ->
                    if (existingContext != null) {
                        return@inTransaction ApplicationError.ContextError.NamingAlreadyExists(
                            attemptedName = command.name
                        ).left()
                    }
                }
            )

            // Create description if provided
            val description = command.description?.let { desc ->
                ContextDescription.create(desc).fold(
                    ifLeft = { errorMsg ->
                        // Convert string error to ApplicationError
                        return@inTransaction ApplicationError.ScopeInputError.DescriptionTooLong(
                            attemptedValue = desc,
                            maximumLength = 500
                        ).left()
                    },
                    ifRight = { it }
                )
            }

            // Create new context view entity
            val now = kotlinx.datetime.Clock.System.now()
            val contextView = ContextView(
                id = ContextViewId.generate(),
                name = contextName,
                filter = contextFilter,
                description = description,
                createdAt = now,
                updatedAt = now
            )

            // Save to repository
            contextViewRepository.save(contextView).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { saved ->
                    // Map to DTO
                    ContextViewInfo(
                        id = saved.id.value,
                        name = saved.name.value,
                        filterExpression = saved.filter.value,
                        description = saved.description?.value,
                        createdAt = saved.createdAt.toString(),
                        updatedAt = saved.updatedAt.toString()
                    ).right()
                }
            )
        }
    }
}