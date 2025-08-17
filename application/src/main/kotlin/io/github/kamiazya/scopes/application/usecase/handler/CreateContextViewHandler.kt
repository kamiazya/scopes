package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.DomainErrorMapper
import io.github.kamiazya.scopes.application.port.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
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
    private val transactionManager: TransactionManager,
    private val logger: Logger
) : UseCase<CreateContextView, ApplicationError, ContextViewResult> {

    override suspend operator fun invoke(input: CreateContextView): Either<ApplicationError, ContextViewResult> = either {
        logger.info("Creating new context view", mapOf(
            "name" to input.name,
            "filterExpression" to input.filterExpression
        ))
        
        transactionManager.inTransaction {
            either {
                // Validate and create context name
                logger.debug("Validating context name", mapOf("name" to input.name))
                val contextName = ContextName.create(input.name).mapLeft { errorMsg ->
                    logger.warn("Invalid context name format", mapOf(
                        "name" to input.name,
                        "error" to errorMsg
                    ))
                    ApplicationError.ContextError.NamingInvalidFormat(
                        attemptedName = input.name
                    )
                }.bind()

                // Validate and create filter
                logger.debug("Validating filter expression", mapOf("filter" to input.filterExpression))
                val contextFilter = ContextFilter.create(input.filterExpression).mapLeft { error ->
                    logger.warn("Invalid filter expression", mapOf(
                        "filter" to input.filterExpression,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    DomainErrorMapper.mapToApplicationError(error)
                }.bind()

                // Check if a context with the same name already exists
                logger.debug("Checking for duplicate context name", mapOf("name" to contextName.value))
                val existingContext = contextViewRepository.findByName(contextName).mapLeft { error ->
                    logger.error("Failed to check existing context", mapOf(
                        "name" to contextName.value,
                        "error" to (error::class.simpleName ?: "Unknown")
                    ))
                    DomainErrorMapper.mapToApplicationError(error)
                }.bind()
                
                ensure(existingContext == null) {
                    logger.warn("Context with same name already exists", mapOf(
                        "name" to input.name
                    ))
                    ApplicationError.ContextError.NamingAlreadyExists(
                        attemptedName = input.name
                    )
                }

                // Create description if provided
                val description = input.description?.let { desc ->
                    logger.debug("Validating description", mapOf("length" to desc.length))
                    ContextDescription.create(desc).mapLeft { errorMsg ->
                        logger.warn("Description validation failed", mapOf(
                            "length" to desc.length,
                            "error" to errorMsg
                        ))
                        ApplicationError.ScopeInputError.DescriptionTooLong(
                            attemptedValue = desc,
                            maximumLength = 500
                        )
                    }.bind()
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
                    DomainErrorMapper.mapToApplicationError(error)
                }.bind()
                
                logger.info("Context view created successfully", mapOf(
                    "id" to saved.id.value,
                    "name" to saved.name.value
                ))

                // Map to DTO
                ContextViewResult(
                    id = saved.id.value,
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