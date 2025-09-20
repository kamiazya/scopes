package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
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
 *
 * Note: This handler returns contract errors directly as part of the
 * architecture simplification to eliminate duplicate error definitions.
 */
class UpdateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val scopeAliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
    private val titleUniquenessSpec: ScopeTitleUniquenessSpecification = ScopeTitleUniquenessSpecification(),
) : CommandHandler<UpdateScopeCommand, ScopeContractError, ScopeDto> {

    override suspend operator fun invoke(command: UpdateScopeCommand): Either<ScopeContractError, ScopeDto> = either {
        logUpdateStart(command)

        executeUpdate(command).bind()
    }.onLeft { error ->
        logUpdateError(error)
    }

    private fun logUpdateStart(command: UpdateScopeCommand) {
        logger.info(
            "Updating scope",
            mapOf(
                "scopeId" to command.id,
                "hasTitle" to (command.title != null).toString(),
                "hasDescription" to (command.description != null).toString(),
            ),
        )
    }

    private fun logUpdateError(error: ScopeContractError) {
        logger.error(
            "Failed to update scope",
            mapOf(
                "code" to getErrorClassName(error),
                "message" to error.toString().take(500),
            ),
        )
    }

    private fun getErrorClassName(error: ScopeContractError): String = error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"

    private suspend fun executeUpdate(command: UpdateScopeCommand): Either<ScopeContractError, ScopeDto> = transactionManager.inTransaction {
        either {
            // Parse scope ID
            val scopeId = ScopeId.create(command.id).mapLeft { error ->
                applicationErrorMapper.mapDomainError(
                    error,
                    ErrorMappingContext(attemptedValue = command.id),
                )
            }.bind()

            // Find existing scope
            val existingScope = findExistingScope(scopeId).bind()

            // Apply updates
            var updatedScope = existingScope

            if (command.title != null) {
                updatedScope = updateTitle(updatedScope, command.title, scopeId).bind()
            }

            if (command.description != null) {
                updatedScope = updateDescription(updatedScope, command.description, scopeId).bind()
            }

            if (command.metadata.isNotEmpty()) {
                updatedScope = updateAspects(updatedScope, command.metadata, scopeId).bind()
            }

            // Save the updated scope
            val savedScope = scopeRepository.save(updatedScope).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error)
            }.bind()
            logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))

            // Fetch aliases to include in the result
            val aliases = scopeAliasRepository.findByScopeId(savedScope.id).mapLeft { error ->
                applicationErrorMapper.mapDomainError(error)
            }.bind()

            ScopeMapper.toDto(savedScope, aliases)
        }
    }

    private suspend fun findExistingScope(scopeId: ScopeId): Either<ScopeContractError, Scope> = either {
        val scope = scopeRepository.findById(scopeId).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error)
        }.bind()
        ensureNotNull(scope) {
            logger.warn("Scope not found for update", mapOf("scopeId" to scopeId.value))
            ScopeContractError.BusinessError.NotFound(scopeId = scopeId.value)
        }
    }

    private suspend fun updateTitle(scope: Scope, newTitle: String, scopeId: ScopeId): Either<ScopeContractError, Scope> = either {
        val title = ScopeTitle.create(newTitle).mapLeft { error ->
            applicationErrorMapper.mapDomainError(
                error,
                ErrorMappingContext(attemptedValue = newTitle),
            )
        }.bind()

        // Use specification to validate title uniqueness
        titleUniquenessSpec.isSatisfiedByForUpdate(
            newTitle = title,
            currentTitle = scope.title,
            parentId = scope.parentId,
            scopeId = scopeId,
            titleExistsChecker = { checkTitle, parentId ->
                scopeRepository.findIdByParentIdAndTitle(parentId, checkTitle.value).getOrNull()
            },
        ).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error)
        }.bind()

        val updated = scope.updateTitle(newTitle, Clock.System.now()).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error)
        }.bind()

        logger.debug(
            "Title updated",
            mapOf(
                "scopeId" to scopeId.value,
                "newTitle" to newTitle,
            ),
        )

        updated
    }

    private fun updateDescription(scope: Scope, newDescription: String, scopeId: ScopeId): Either<ScopeContractError, Scope> = either {
        val updated = scope.updateDescription(newDescription, Clock.System.now())
            .mapLeft { error ->
                applicationErrorMapper.mapDomainError(
                    error,
                    ErrorMappingContext(attemptedValue = newDescription),
                )
            }.bind()

        logger.debug(
            "Description updated",
            mapOf(
                "scopeId" to scopeId.value,
                "hasDescription" to newDescription.isNotEmpty().toString(),
            ),
        )

        updated
    }

    private fun updateAspects(scope: Scope, metadata: Map<String, String>, scopeId: ScopeId): Either<ScopeContractError, Scope> = either {
        val aspects = buildAspects(metadata)
        val updated = scope.updateAspects(Aspects.from(aspects), Clock.System.now())

        logger.debug(
            "Aspects updated",
            mapOf(
                "scopeId" to scopeId.value,
                "aspectCount" to aspects.size.toString(),
            ),
        )

        updated
    }

    private fun buildAspects(metadata: Map<String, String>): Map<AspectKey, NonEmptyList<AspectValue>> = metadata.mapNotNull { (key, value) ->
        val aspectKey = AspectKey.create(key).getOrNull()
        val aspectValue = AspectValue.create(value).getOrNull()
        if (aspectKey != null && aspectValue != null) {
            aspectKey to nonEmptyListOf(aspectValue)
        } else {
            logger.debug("Skipping invalid aspect", mapOf("key" to key, "value" to value))
            null
        }
    }.toMap()
}
