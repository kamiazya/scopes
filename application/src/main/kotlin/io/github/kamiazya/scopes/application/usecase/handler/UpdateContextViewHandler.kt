package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ContextError
import io.github.kamiazya.scopes.application.error.ScopeInputError
import io.github.kamiazya.scopes.application.error.toApplicationError
import io.github.kamiazya.scopes.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.UpdateContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription

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
    private val transactionManager: TransactionManager,
    private val logger: Logger
) : UseCase<UpdateContextView, ApplicationError, ContextViewResult> {

    override suspend operator fun invoke(input: UpdateContextView): Either<ApplicationError, ContextViewResult> = either {
        logger.info("Updating context view", mapOf(
            "id" to input.id,
            "hasName" to (input.name != null),
            "hasFilter" to (input.filterExpression != null),
            "hasDescription" to (input.description != null)
        ))

        transactionManager.inTransaction {
            either {
                // Parse and validate the context ID
                logger.debug("Parsing context ID", mapOf("id" to input.id))
                val contextId = ContextViewId.create(input.id).mapLeft { error ->
                    logger.warn("Invalid context ID format", mapOf(
                        "id" to input.id,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    error.toGenericApplicationError()
                }.bind()

                // Find the existing context view
                logger.debug("Finding existing context", mapOf("id" to contextId.value))
                val existingContext = contextViewRepository.findById(contextId).mapLeft { error ->
                    logger.error("Failed to find context", mapOf(
                        "id" to contextId.value,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    error.toGenericApplicationError()
                }.bind()

                ensure(existingContext != null) {
                    logger.warn("Context not found", mapOf("id" to input.id))
                    ContextError.StateNotFound(
                        contextId = input.id
                    )
                }

                // Update name if provided
                val newName = input.name?.let { name ->
                    logger.debug("Validating new name", mapOf("name" to name))
                    ContextViewName.create(name).mapLeft { errorMsg ->
                        logger.warn("Invalid name format", mapOf("name" to name))
                        when {
                            errorMsg.contains("empty", ignoreCase = true) -> ContextError.NameEmpty
                            errorMsg.contains("exceed", ignoreCase = true) || errorMsg.contains("too long", ignoreCase = true) -> 
                                ContextError.NameTooLong(
                                    attemptedName = name,
                                    maximumLength = 100
                                )
                            else -> ContextError.NameInvalidFormat(attemptedName = name)
                        }
                    }.bind()
                } ?: existingContext.name

                // Name changes are allowed without uniqueness check since only keys must be unique

                // Update filter if provided
                val newFilter = input.filterExpression?.let { filter ->
                    logger.debug("Validating new filter", mapOf("filter" to filter))
                    ContextViewFilter.create(filter).mapLeft { errorMsg ->
                        logger.warn("Invalid filter expression", mapOf(
                            "filter" to filter,
                            "error" to errorMsg
                        ))
                        ContextError.FilterInvalidSyntax(
                            position = 0,
                            reason = errorMsg,
                            expression = filter
                        )
                    }.bind()
                } ?: existingContext.filter

                // Update description if provided
                val newDescription = when {
                    input.description == null -> {
                        logger.debug("Keeping existing description")
                        existingContext.description
                    }
                    input.description.isEmpty() -> {
                        logger.debug("Clearing description")
                        null
                    }
                    else -> {
                        logger.debug("Validating new description", mapOf("length" to input.description.length))
                        ContextViewDescription.create(input.description).mapLeft {
                            logger.warn("Description validation failed", mapOf(
                                "length" to input.description.length
                            ))
                            ScopeInputError.DescriptionTooLong(
                                attemptedValue = input.description,
                                maximumLength = 500
                            )
                        }.bind()
                    }
                }

                // Create updated context view
                val now = kotlinx.datetime.Clock.System.now()
                val updatedContext = existingContext.copy(
                    name = newName,
                    filter = newFilter,
                    description = newDescription,
                    updatedAt = now
                )

                logger.debug("Saving updated context", mapOf(
                    "id" to updatedContext.id.value,
                    "name" to updatedContext.name.value
                ))

                // Save the updated context
                val saved = contextViewRepository.save(updatedContext).mapLeft { error ->
                    logger.error("Failed to save updated context", mapOf(
                        "id" to updatedContext.id.value,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    error.toGenericApplicationError()
                }.bind()

                logger.info("Context view updated successfully", mapOf(
                    "id" to saved.id.value,
                    "name" to saved.name.value
                ))

                // Map to DTO
                ContextViewResult(
                    id = saved.id.value,
                    key = saved.key.value,
                    name = saved.name.value,
                    filterExpression = saved.filter.value,
                    description = saved.description?.value,
                    isActive = false, // Active status should be checked separately
                    createdAt = saved.createdAt,
                    updatedAt = saved.updatedAt
                )
            }
        }.bind()
    }.onLeft { error ->
        logger.error("Failed to update context view", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}
