package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.UpdateScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
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
) : CommandHandler<UpdateScopeCommand, ScopeManagementApplicationError, ScopeDto> {

    override suspend operator fun invoke(command: UpdateScopeCommand): Either<ScopeManagementApplicationError, ScopeDto> = either {
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

    private fun logUpdateError(error: ScopeManagementApplicationError) {
        logger.error(
            "Failed to update scope",
            mapOf(
                "code" to getErrorClassName(error),
                "message" to error.toString().take(500),
            ),
        )
    }

    private fun getErrorClassName(error: ScopeManagementApplicationError): String = error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"

    private suspend fun executeUpdate(command: UpdateScopeCommand): Either<ScopeManagementApplicationError, ScopeDto> = transactionManager.inTransaction {
        performUpdate(command)
    }

    private suspend fun performUpdate(command: UpdateScopeCommand): Either<ScopeManagementApplicationError, ScopeDto> = either {
        val scopeId = parseScopeId(command.id).bind()
        val existingScope = findExistingScope(scopeId).bind()

        val updatedScope = applyUpdates(existingScope, command, scopeId).bind()

        val savedScope = scopeRepository.save(updatedScope).mapLeft { it.toGenericApplicationError() }.bind()
        logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))

        ScopeMapper.toDto(savedScope)
    }

    private fun parseScopeId(id: String): Either<ScopeManagementApplicationError, ScopeId> = ScopeId.create(id).mapLeft { error ->
        mapIdError(error, id)
    }

    private fun mapIdError(error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError, id: String): ScopeManagementApplicationError =
        when (error) {
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError.EmptyId ->
                ScopeInputError.IdBlank(id)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError.InvalidIdFormat ->
                ScopeInputError.IdInvalidFormat(id, "${error.expectedFormat} format")
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
        val scope = scopeRepository.findById(scopeId).mapLeft { it.toGenericApplicationError() }.bind()
        ensureNotNull(scope) {
            logger.warn("Scope not found for update", mapOf("scopeId" to scopeId.value))
            ScopeNotFoundError(scopeId = scopeId).toGenericApplicationError()
        }
    }

    private suspend fun updateTitle(scope: Scope, newTitle: String, scopeId: ScopeId): Either<ScopeManagementApplicationError, Scope> = either {
        val title = ScopeTitle.create(newTitle).mapLeft { error ->
            when (error) {
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError.EmptyTitle ->
                    ScopeInputError.TitleEmpty(newTitle)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError.TitleTooShort ->
                    ScopeInputError.TitleTooShort(newTitle, error.minLength)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError.TitleTooLong ->
                    ScopeInputError.TitleTooLong(newTitle, error.maxLength)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.TitleError.InvalidTitleFormat ->
                    ScopeInputError.TitleContainsProhibitedCharacters(newTitle, listOf('<', '>', '&', '"'))
            }
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
        ).mapLeft { it.toGenericApplicationError() }.bind()

        val updated = scope.updateTitle(newTitle, Clock.System.now()).mapLeft { it.toGenericApplicationError() }.bind()

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
                    is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.DescriptionError.DescriptionTooLong ->
                        ScopeInputError.DescriptionTooLong(newDescription, error.maxLength)
                    else -> error.toGenericApplicationError()
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
