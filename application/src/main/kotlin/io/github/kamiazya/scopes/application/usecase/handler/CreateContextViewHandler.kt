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
import io.github.kamiazya.scopes.domain.error.PersistenceError as DomainPersistenceError
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.CreateContextView
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription

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
    private val transactionManager: TransactionManager,
    private val logger: Logger
) : UseCase<CreateContextView, ApplicationError, ContextViewResult> {

    override suspend operator fun invoke(input: CreateContextView): Either<ApplicationError, ContextViewResult> = either {
        logger.info("Creating new context view", mapOf(
            "key" to input.key,
            "name" to input.name,
            "filterExpression" to input.filterExpression
        ))

        transactionManager.inTransaction {
            either {
                // Validate and create context key
                logger.debug("Validating context key", mapOf("key" to input.key))
                val contextKey = ContextViewKey.create(input.key).mapLeft { errorMsg ->
                    logger.warn("Invalid context key format", mapOf(
                        "key" to input.key,
                        "error" to errorMsg
                    ))
                    when {
                        errorMsg.contains("empty", ignoreCase = true) -> ContextError.KeyEmpty
                        errorMsg.contains("already exists", ignoreCase = true) -> ContextError.KeyAlreadyExists(
                            attemptedKey = input.key
                        )
                        else -> ContextError.KeyInvalidFormat(
                            attemptedKey = input.key
                        )
                    }
                }.bind()

                // Validate and create context name
                logger.debug("Validating context name", mapOf("name" to input.name))
                val contextName = ContextViewName.create(input.name).mapLeft { errorMsg ->
                    logger.warn("Invalid context name format", mapOf(
                        "name" to input.name,
                        "error" to errorMsg
                    ))
                    when {
                        errorMsg.contains("empty", ignoreCase = true) -> ContextError.NameEmpty
                        errorMsg.contains("exceed", ignoreCase = true) || errorMsg.contains("too long", ignoreCase = true) -> 
                            ContextError.NameTooLong(
                                attemptedName = input.name,
                                maximumLength = 100
                            )
                        else -> ContextError.NameInvalidFormat(attemptedName = input.name)
                    }
                }.bind()

                // Validate and create filter
                logger.debug("Validating filter expression", mapOf("filter" to input.filterExpression))
                val contextFilter = ContextViewFilter.create(input.filterExpression).mapLeft { errorMsg ->
                    logger.warn("Invalid filter expression", mapOf(
                        "filter" to input.filterExpression,
                        "error" to errorMsg
                    ))
                    ContextError.FilterInvalidSyntax(
                        position = 0,
                        reason = errorMsg,
                        expression = input.filterExpression
                    )
                }.bind()

                // Check if a context with the same key already exists
                logger.debug("Checking for duplicate context key", mapOf("key" to contextKey.value))
                val existingContext = contextViewRepository.findByKey(contextKey).mapLeft { error ->
                    logger.error("Failed to check existing context", mapOf(
                        "key" to contextKey.value,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    error.toApplicationError()
                }.bind()

                ensure(existingContext == null) {
                    logger.warn("Context with same key already exists", mapOf(
                        "key" to input.key
                    ))
                    ContextError.KeyAlreadyExists(
                        attemptedKey = input.key
                    )
                }

                // Create description if provided
                val description = input.description?.let { desc ->
                    logger.debug("Validating description", mapOf("length" to desc.length))
                    ContextViewDescription.create(desc).mapLeft { errorMsg ->
                        logger.warn("Description validation failed", mapOf(
                            "length" to desc.length,
                            "error" to errorMsg
                        ))
                        ScopeInputError.DescriptionTooLong(
                            attemptedValue = desc,
                            maximumLength = 500
                        )
                    }.bind()
                }

                // Create new context view entity
                val now = kotlinx.datetime.Clock.System.now()
                val contextView = ContextView(
                    id = ContextViewId.generate(),
                    key = contextKey,
                    name = contextName,
                    filter = contextFilter,
                    description = description,
                    createdAt = now,
                    updatedAt = now
                )

                logger.debug("Saving context view", mapOf(
                    "id" to contextView.id.value,
                    "name" to contextView.name.value
                ))

                // Save to repository
                val saved = contextViewRepository.save(contextView).mapLeft { error ->
                    logger.error("Failed to save context view", mapOf(
                        "id" to contextView.id.value,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    error.toApplicationError()
                }.bind()

                logger.info("Context view created successfully", mapOf(
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
                    isActive = false, // Newly created context is not active by default
                    createdAt = saved.createdAt,
                    updatedAt = saved.updatedAt
                )
            }
        }.bind()
    }.onLeft { error ->
        logger.error("Failed to create context view", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}
