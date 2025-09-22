package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock

/**
 * Handler for updating an existing scope.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 */
class UpdateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<UpdateScopeCommand, ScopeDto>(transactionManager, logger) {

    override suspend fun executeCommand(command: UpdateScopeCommand): Either<ScopeContractError, ScopeDto> = either {
        // Parse scope ID
        val scopeId = ScopeId.create(command.id).mapLeft { error ->
            logger.warn("Invalid scope ID", mapOf("id" to command.id))
            applicationErrorMapper.mapDomainError(error, ErrorMappingContext(attemptedValue = command.id))
        }.bind()

        // Find existing scope
        val existingScope = scopeRepository.findById(scopeId).mapLeft { error ->
            logger.error("Repository error", mapOf("scopeId" to command.id))
            applicationErrorMapper.mapDomainError(error)
        }.bind()
        ensureNotNull(existingScope) {
            logger.warn("Scope not found", mapOf("scopeId" to command.id))
            ScopeContractError.BusinessError.NotFound(scopeId = command.id)
        }
        logger.debug("Found existing scope", mapOf("scopeId" to command.id))

        // If updating title, check uniqueness
        val updatedTitle = if (command.title != null && command.title != existingScope.title.value) {
            val newTitle = ScopeTitle.create(command.title).mapLeft { error ->
                logger.warn("Invalid title format", mapOf("title" to command.title))
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext(attemptedValue = command.title))
            }.bind()

            // Check title uniqueness at the same level
            val existingScopeIdWithTitle = scopeRepository.findIdByParentIdAndTitle(
                existingScope.parentId,
                newTitle.value,
            ).mapLeft { error ->
                logger.error(
                    "Failed to check title uniqueness",
                    mapOf(
                        "title" to command.title,
                        "parentId" to (existingScope.parentId?.toString() ?: "null"),
                        "error" to error.toString(),
                    ),
                )
                when (error) {
                    is io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError ->
                        applicationErrorMapper.mapDomainError(error)
                    else -> ScopeContractError.SystemError.ServiceUnavailable(service = "scope-repository")
                }
            }.bind()

            // If another scope with same title exists (and it's not this scope), it's a conflict
            if (existingScopeIdWithTitle != null && existingScopeIdWithTitle != existingScope.id) {
                logger.warn(
                    "Title uniqueness violation",
                    mapOf(
                        "title" to command.title,
                        "parentId" to (existingScope.parentId?.toString() ?: "null"),
                        "conflictingScopeId" to existingScopeIdWithTitle.toString(),
                    ),
                )
                raise(
                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = command.title,
                        parentId = existingScope.parentId?.toString(),
                        existingScopeId = existingScopeIdWithTitle.toString(),
                    ),
                )
            }

            newTitle
        } else {
            existingScope.title
        }

        // Process description if provided
        val updatedDescription = command.description?.let { desc ->
            io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription.create(desc).mapLeft { error ->
                logger.warn("Invalid description format", mapOf("description" to desc))
                applicationErrorMapper.mapDomainError(error, ErrorMappingContext(attemptedValue = desc))
            }.bind()
        } ?: existingScope.description

        // Create updated scope
        val updatedScope = existingScope.copy(
            title = updatedTitle,
            description = updatedDescription,
            updatedAt = Clock.System.now(),
        )

        // Save updated scope
        val savedScope = scopeRepository.update(updatedScope).mapLeft { error ->
            logger.error("Failed to update scope", mapOf("scopeId" to command.id))
            applicationErrorMapper.mapDomainError(error)
        }.bind()

        // Map to DTO and return
        logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))
        ScopeMapper.toDto(savedScope)
    }
}
