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
                // Parse scope ID
                val scopeId = ScopeId.create(command.id).bind()

                // Find existing scope
                val existingScope = ensureNotNull(scopeRepository.findById(scopeId).bind()) {
                    logger.warn("Scope not found for update", mapOf("scopeId" to command.id))
                    ScopeNotFoundError(
                        scopeId = scopeId,
                        occurredAt = Clock.System.now(),
                    )
                }

                var updatedScope = existingScope

                // Update title if provided
                if (command.title != null) {
                    val newTitle = ScopeTitle.create(command.title).bind()

                    // Use specification to validate title uniqueness
                    titleUniquenessSpec.isSatisfiedByForUpdate(
                        newTitle = newTitle,
                        currentTitle = existingScope.title,
                        parentId = existingScope.parentId,
                        scopeId = scopeId,
                        titleExistsChecker = { title, parentId ->
                            scopeRepository.findIdByParentIdAndTitle(parentId, title.value).bind()
                        },
                    ).bind()

                    updatedScope = updatedScope.updateTitle(command.title, Clock.System.now()).bind()
                    logger.debug(
                        "Title updated",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "newTitle" to command.title,
                        ),
                    )
                }

                // Update description if provided
                if (command.description != null) {
                    updatedScope = updatedScope.updateDescription(command.description, Clock.System.now()).bind()
                    logger.debug(
                        "Description updated",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "hasDescription" to command.description.isNotEmpty().toString(),
                        ),
                    )
                }

                // Update metadata/aspects if provided
                if (command.metadata.isNotEmpty()) {
                    val aspects = command.metadata.mapNotNull { (key, value) ->
                        val aspectKey = AspectKey.create(key).getOrNull()
                        val aspectValue = AspectValue.create(value).getOrNull()
                        if (aspectKey != null && aspectValue != null) {
                            aspectKey to nonEmptyListOf(aspectValue)
                        } else {
                            logger.debug("Skipping invalid aspect", mapOf("key" to key, "value" to value))
                            null
                        }
                    }.toMap()

                    updatedScope = updatedScope.updateAspects(Aspects.from(aspects), Clock.System.now())
                    logger.debug(
                        "Aspects updated",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "aspectCount" to aspects.size.toString(),
                        ),
                    )
                }

                // Save the updated scope
                val savedScope = scopeRepository.save(updatedScope).bind()
                logger.info("Scope updated successfully", mapOf("scopeId" to savedScope.id.value))

                ScopeMapper.toDto(savedScope)
            }
        }.bind()
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
