package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
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
 */
class UpdateScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
    private val titleUniquenessSpec: ScopeTitleUniquenessSpecification = ScopeTitleUniquenessSpecification(),
) : CommandHandler<UpdateScopeCommand, ScopesError, ScopeDto> {

    override suspend operator fun invoke(command: UpdateScopeCommand): Either<ScopesError, ScopeDto> = either {
        logger.info(
            "Updating scope",
            mapOf(
                "scopeId" to command.id,
                "hasTitle" to (command.title != null).toString(),
                "hasDescription" to (command.description != null).toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                val scopeId = ScopeId.create(command.id).bind()
                val existingScope = findExistingScope(scopeId, command.id).bind()
                val updatedScope = applyUpdates(existingScope, command, scopeId).bind()
                val savedScope = scopeRepository.save(updatedScope).bind()

                logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))
                ScopeMapper.toDto(savedScope)
            }
        }.bind()
    }

    private suspend fun findExistingScope(scopeId: ScopeId, commandId: String): Either<ScopesError, ScopeAggregate> = either {
        ensureNotNull(scopeRepository.findById(scopeId).bind()) {
            logger.warn("Scope not found for update", mapOf("scopeId" to commandId))
            ScopeNotFoundError(
                scopeId = scopeId,
                occurredAt = Clock.System.now(),
            )
        }
    }

    private suspend fun applyUpdates(scope: ScopeAggregate, command: UpdateScopeCommand, scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        var updatedScope = scope

        if (command.title != null) {
            updatedScope = updateTitle(updatedScope, command.title, scopeId).bind()
        }

        if (command.description != null) {
            updatedScope = updateDescription(updatedScope, command.description, scopeId).bind()
        }

        if (command.metadata.isNotEmpty()) {
            updatedScope = updateAspects(updatedScope, command.metadata, scopeId).bind()
        }

        updatedScope
    }

    private suspend fun updateTitle(scope: ScopeAggregate, newTitle: String, scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        val title = ScopeTitle.create(newTitle).bind()

        titleUniquenessSpec.isSatisfiedByForUpdate(
            newTitle = title,
            currentTitle = scope.title,
            parentId = scope.parentId,
            scopeId = scopeId,
            titleExistsChecker = { checkTitle, parentId ->
                scopeRepository.findIdByParentIdAndTitle(parentId, checkTitle.value).bind()
            },
        ).bind()

        val updated = scope.updateTitle(newTitle, Clock.System.now()).bind()
        logger.debug(
            "Title updated",
            mapOf("scopeId" to scopeId.value, "newTitle" to newTitle),
        )
        updated
    }

    private fun updateDescription(scope: ScopeAggregate, newDescription: String, scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        val updated = scope.updateDescription(newDescription, Clock.System.now()).bind()
        logger.debug(
            "Description updated",
            mapOf(
                "scopeId" to scopeId.value,
                "hasDescription" to newDescription.isNotEmpty().toString(),
            ),
        )
        updated
    }

    private fun updateAspects(scope: ScopeAggregate, metadata: Map<String, String>, scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        val aspects = metadata.mapNotNull { (key, value) ->
            val aspectKey = AspectKey.create(key).getOrNull()
            val aspectValue = AspectValue.create(value).getOrNull()
            if (aspectKey != null && aspectValue != null) {
                aspectKey to nonEmptyListOf(aspectValue)
            } else {
                logger.debug("Skipping invalid aspect", mapOf("key" to key, "value" to value))
                null
            }
        }.toMap()

        val updated = scope.updateAspects(Aspects.from(aspects), Clock.System.now())
        logger.debug(
            "Aspects updated",
            mapOf("scopeId" to scopeId.value, "aspectCount" to aspects.size.toString()),
        )
        updated
    }.onLeft { error ->
        logger.error(
            "Failed to update scope",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
