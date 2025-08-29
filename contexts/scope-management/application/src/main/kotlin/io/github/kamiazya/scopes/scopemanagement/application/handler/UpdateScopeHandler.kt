package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
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
) : UseCase<UpdateScope, ScopesError, ScopeDto> {

    override suspend operator fun invoke(input: UpdateScope): Either<ScopesError, ScopeDto> = either {
        logger.info(
            "Updating scope",
            mapOf(
                "scopeId" to input.id,
                "hasTitle" to (input.title != null).toString(),
                "hasDescription" to (input.description != null).toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                // Parse scope ID
                val scopeId = ScopeId.create(input.id).bind()

                // Find existing scope
                val existingScope = scopeRepository.findById(scopeId).bind()
                ensure(existingScope != null) {
                    logger.warn("Scope not found for update", mapOf("scopeId" to input.id))
                    ScopeNotFoundError(
                        scopeId = scopeId,
                        occurredAt = Clock.System.now(),
                    )
                }

                var updatedScope = existingScope

                // Update title if provided
                if (input.title != null) {
                    val newTitle = ScopeTitle.create(input.title).bind()

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

                    updatedScope = updatedScope.updateTitle(input.title).bind()
                    logger.debug(
                        "Title updated",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "newTitle" to input.title,
                        ),
                    )
                }

                // Update description if provided
                if (input.description != null) {
                    updatedScope = updatedScope.updateDescription(input.description).bind()
                    logger.debug(
                        "Description updated",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "hasDescription" to input.description.isNotEmpty().toString(),
                        ),
                    )
                }

                // Update metadata/aspects if provided
                if (input.metadata.isNotEmpty()) {
                    val aspects = input.metadata.mapNotNull { (key, value) ->
                        val aspectKey = AspectKey.create(key).getOrNull()
                        val aspectValue = AspectValue.create(value).getOrNull()
                        if (aspectKey != null && aspectValue != null) {
                            aspectKey to nonEmptyListOf(aspectValue)
                        } else {
                            logger.debug("Skipping invalid aspect", mapOf("key" to key, "value" to value))
                            null
                        }
                    }.toMap()

                    updatedScope = updatedScope.updateAspects(Aspects.from(aspects))
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
                "error" to (error::class.simpleName ?: "Unknown"),
                "message" to error.toString(),
            ),
        )
    }
}
