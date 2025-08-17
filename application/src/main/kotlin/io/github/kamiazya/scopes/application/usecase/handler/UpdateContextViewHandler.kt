package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.UpdateContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextDescription

/**
 * Handler for updating an existing context view.
 * 
 * This handler:
 * 1. Validates the context ID and finds the existing context
 * 2. Updates only the provided fields (name, filter, description)
 * 3. Validates the new values before updating
 * 4. Persists the changes to the repository
 * 5. Returns the updated context view information
 */
class UpdateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val transactionManager: TransactionManager
) : UseCase<UpdateContextView, ApplicationError, ContextViewResult> {

    override suspend operator fun invoke(input: UpdateContextView): Either<ApplicationError, ContextViewResult> {
        return transactionManager.inTransaction {
            // Parse and validate the context ID
            val contextId = ContextViewId.create(input.id).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { it }
            )

            // Find the existing context view
            val existingContext = contextViewRepository.findById(contextId).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { context ->
                    context ?: return@inTransaction ApplicationError.ContextError.StateNotFound(
                        contextId = input.id
                    ).left()
                }
            )

            // Update name if provided
            val newName = input.name?.let { name ->
                ContextName.create(name).fold(
                    ifLeft = { 
                        return@inTransaction ApplicationError.ContextError.NamingInvalidFormat(
                            attemptedName = name
                        ).left()
                    },
                    ifRight = { it }
                )
            } ?: existingContext.name

            // Check if new name would cause a duplicate
            if (input.name != null && newName.value != existingContext.name.value) {
                contextViewRepository.findByName(newName).fold(
                    ifLeft = { error -> 
                        return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                    },
                    ifRight = { existing ->
                        if (existing != null && existing.id != contextId) {
                            return@inTransaction ApplicationError.ContextError.NamingAlreadyExists(
                                attemptedName = input.name
                            ).left()
                        }
                    }
                )
            }

            // Update filter if provided
            val newFilter = input.filterExpression?.let { filter ->
                ContextFilter.create(filter).fold(
                    ifLeft = { error -> 
                        return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                    },
                    ifRight = { it }
                )
            } ?: existingContext.filter

            // Update description if provided
            val newDescription = when {
                input.description == null -> existingContext.description // Not provided, keep existing
                input.description.isEmpty() -> null // Empty string means clear description
                else -> ContextDescription.create(input.description).fold(
                    ifLeft = { 
                        return@inTransaction ApplicationError.ScopeInputError.DescriptionTooLong(
                            attemptedValue = input.description,
                            maximumLength = 500
                        ).left()
                    },
                    ifRight = { it }
                )
            }

            // Create updated context view
            val now = kotlinx.datetime.Clock.System.now()
            val updatedContext = existingContext.copy(
                name = newName,
                filter = newFilter,
                description = newDescription,
                updatedAt = now
            )

            // Save the updated context
            contextViewRepository.save(updatedContext).fold(
                ifLeft = { error -> 
                    return@inTransaction DomainErrorMapper.mapToApplicationError(error).left()
                },
                ifRight = { saved ->
                    // Map to DTO
                    ContextViewResult(
                        id = saved.id.value,
                        name = saved.name.value,
                        filterExpression = saved.filter.value,
                        description = saved.description?.value,
                        isActive = false, // Active status should be checked separately
                        createdAt = saved.createdAt,
                        updatedAt = saved.updatedAt
                    ).right()
                }
            )
        }
    }
}