package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.UpdateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for updating an existing context view.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 *
 * This handler validates the input, retrieves the existing context view,
 * applies the updates, and persists the changes.
 */
class UpdateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<UpdateContextViewCommand, ContextViewDto>(transactionManager, logger) {

    override suspend fun executeCommand(command: UpdateContextViewCommand): Either<ScopeContractError, ContextViewDto> = either {
        logger.info(
            "Updating context view",
            mapOf(
                "key" to command.key,
                "name" to command.name,
                "filter" to command.filter,
                "description" to command.description,
            ),
        )
        
        // Validate and create key value object
        val key = ContextViewKey.create(command.key)
            .mapLeft { error ->
                logger.error(
                    "Invalid context view key",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        // Retrieve existing context view
        val existingContextView = contextViewRepository.findByKey(key).fold(
            { error ->
                logger.error(
                    "Failed to find context view",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                raise(
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = "context-view-repository",
                    ),
                )
            },
            { context ->
                if (context == null) {
                    logger.error(
                        "Context view not found",
                        mapOf("key" to command.key),
                    )
                    raise(
                        ScopeContractError.BusinessError.ContextNotFound(
                            contextKey = command.key,
                        ),
                    )
                }
                context
            },
        )

        // Prepare updated values
        val updatedName = command.name?.let { newName ->
            ContextViewName.create(newName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid context view name",
                        mapOf(
                            "name" to newName,
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()
        } ?: existingContextView.name

        val updatedFilter = command.filter?.let { newFilter ->
            ContextViewFilter.create(newFilter)
                .mapLeft { error ->
                    logger.error(
                        "Invalid context view filter",
                        mapOf(
                            "filter" to newFilter,
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapDomainError(error)
                }
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
                    .mapLeft { error ->
                        logger.error(
                            "Failed to clear context view description",
                            mapOf(
                                "key" to command.key,
                                "error" to error.toString(),
                            ),
                        )
                        applicationErrorMapper.mapDomainError(error)
                    }
                    .bind()
            } else {
                updatedContextView.updateDescription(command.description, Clock.System.now())
                    .mapLeft { error ->
                        logger.error(
                            "Failed to update context view description",
                            mapOf(
                                "key" to command.key,
                                "description" to command.description,
                                "error" to error.toString(),
                            ),
                        )
                        applicationErrorMapper.mapDomainError(error)
                    }
                    .bind()
            }
        }

        // Save to repository (using save method for updates)
        val saved = contextViewRepository.save(updatedContextView).fold(
            { error ->
                logger.error(
                    "Failed to save updated context view",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                raise(
                    ScopeContractError.SystemError.ServiceUnavailable(
                        service = "context-view-repository",
                    ),
                )
            },
            { saved -> saved },
        )

        logger.info(
            "Context view updated successfully",
            mapOf(
                "key" to saved.key.value,
                "id" to saved.id.value.toString(),
            ),
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