package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
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
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class UpdateScopeHandler(
    private val scopeRepository: ScopeRepository,
    transactionManager: TransactionManager,
    logger: Logger,
    private val titleUniquenessSpec: ScopeTitleUniquenessSpecification = ScopeTitleUniquenessSpecification(),
) : BaseCommandHandler<UpdateScopeCommand, ScopeDto>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: UpdateScopeCommand): Either<ScopeManagementApplicationError, ScopeDto> = either {
        val scopeId = parseScopeId(command.id).bind()
        val existingScope = findExistingScope(scopeId).bind()

        val updatedScope = applyUpdates(existingScope, command, scopeId).bind()

        val savedScope = scopeRepository.save(updatedScope).mapLeft {
            errorMappingService.mapRepositoryError(it, "update-scope-save")
        }.bind()
        logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))

        ScopeMapper.toDto(savedScope)
    }

    private fun parseScopeId(id: String): Either<ScopeManagementApplicationError, ScopeId> = ScopeId.create(id).mapLeft { error ->
        errorMappingService.mapScopeIdError(error, id, "update-scope")
    }

    private suspend fun applyUpdates(scope: Scope, command: UpdateScopeCommand, scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
        var updatedScope = scope

        command.title?.let { title ->
            updatedScope = updateTitle(updatedScope, title, scopeId).bind()
        }

        command.description?.let { description ->
            updatedScope = updateDescription(updatedScope, description, scopeId).bind()
        }

        if (command.metadata.isNotEmpty()) {
            updatedScope = updateAspects(updatedScope, command.metadata, scopeId).bind()
        }

        updatedScope
    }

    private suspend fun findExistingScope(scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
        val scope = scopeRepository.findById(scopeId).mapLeft {
            errorMappingService.mapRepositoryError(it, "find-scope-for-update")
        }.bind()
        ensureNotNull(scope) {
            logger.warn("Scope not found for update", mapOf("scopeId" to scopeId.value))
            errorMappingService.mapScopeNotFoundError(scopeId, "update-scope")
        }
    }

    private suspend fun updateTitle(scope: Scope, newTitle: String, scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
        val title = ScopeTitle.create(newTitle).mapLeft { error ->
            errorMappingService.mapTitleError(error, newTitle, "update-scope-title")
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
        ).mapLeft {
            errorMappingService.mapDomainError(it, "update-scope-title-uniqueness")
        }.bind()

        val updated = scope.updateTitle(newTitle, Clock.System.now()).mapLeft {
            errorMappingService.mapDomainError(it, "update-scope-title-entity")
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

    private fun updateDescription(scope: Scope, newDescription: String, scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
        val updated = scope.updateDescription(newDescription, Clock.System.now())
            .mapLeft { error ->
                when (error) {
                    is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.DescriptionError ->
                        errorMappingService.mapDescriptionError(error, newDescription)
                    else -> errorMappingService.mapDomainError(error, "update-scope-description")
                }
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

    private fun updateAspects(scope: Scope, metadata: Map<String, String>, scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
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
