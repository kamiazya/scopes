package io.github.kamiazya.scopes.scopemanagement.application.command.handler.context

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.context.CreateContextViewCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock

/**
 * Handler for creating a new context view.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 *
 * This handler validates the input, ensures the key is unique,
 * validates the filter syntax, and persists the new context view.
 */
class CreateContextViewHandler(
    private val contextViewRepository: ContextViewRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<CreateContextViewCommand, ContextViewDto>(transactionManager, logger) {

    override suspend fun executeCommand(command: CreateContextViewCommand): Either<ScopeContractError, ContextViewDto> = either {
        logger.info(
            "Creating new context view",
            mapOf(
                "key" to command.key,
                "name" to command.name,
                "filter" to command.filter,
            ),
        )
        
        // Validate and create value objects
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
            
        val name = ContextViewName.create(command.name)
            .mapLeft { error ->
                logger.error(
                    "Invalid context view name",
                    mapOf(
                        "name" to command.name,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()
            
        val filter = ContextViewFilter.create(command.filter)
            .mapLeft { error ->
                logger.error(
                    "Invalid context view filter",
                    mapOf(
                        "filter" to command.filter,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        // Create the context view
        val contextView = ContextView.create(
            key = key,
            name = name,
            filter = filter,
            description = command.description,
            now = Clock.System.now(),
        ).mapLeft { error ->
            logger.error(
                "Failed to create context view entity",
                mapOf(
                    "key" to command.key,
                    "error" to error.toString(),
                ),
            )
            applicationErrorMapper.mapDomainError(error)
        }.bind()

        // Save to repository
        val saved = contextViewRepository.save(contextView)
            .mapLeft { error ->
                logger.error(
                    "Failed to save context view",
                    mapOf(
                        "key" to command.key,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        logger.info(
            "Context view created successfully",
            mapOf(
                "id" to saved.id.value.toString(),
                "key" to saved.key.value,
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
