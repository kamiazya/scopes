package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.specification.ScopeTitleUniquenessSpecification
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
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
    private val titleUniquenessSpec: ScopeTitleUniquenessSpecification = ScopeTitleUniquenessSpecification(),
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
                applicationErrorMapper.mapDomainError(error)
            }.bind()

            // Check title uniqueness
            titleUniquenessSpec.isSatisfiedBy(
                newTitle.value,
                existingScope.parentId,
                excludeId = existingScope.id,
            ).mapLeft { error ->
                logger.warn(
                    "Title uniqueness violation",
                    mapOf(
                        "title" to command.title,
                        "parentId" to (existingScope.parentId?.toString() ?: "null"),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }.bind()

            newTitle
        } else {
            existingScope.title
        }

        // Process aspects if provided
        val updatedAspects = if (!command.aspects.isNullOrEmpty()) {
            val aspectsList = command.aspects.flatMap { (key, values) ->
                values.map { value ->
                    AspectKey.create(key).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error)
                    }.bind() to AspectValue.create(value).mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error)
                    }.bind()
                }
            }
            val aspectMap = aspectsList.groupBy({ it.first }, { it.second }).mapValues { (_, values) ->
                NonEmptyList.fromListUnsafe(values)
            }
            Aspects.from(aspectMap)
        } else {
            existingScope.aspects
        }

        // Create updated scope
        val updatedScope = existingScope.copy(
            title = updatedTitle,
            aspects = updatedAspects,
            updatedAt = Clock.System.now(),
        )

        // Save updated scope
        val savedScope = scopeRepository.update(updatedScope).mapLeft { error ->
            logger.error("Failed to update scope", mapOf("scopeId" to command.id))
            applicationErrorMapper.mapDomainError(error)
        }.bind()

        // Map to DTO and return
        logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))
        ScopeMapper.toScopeDto(savedScope)
    }
}