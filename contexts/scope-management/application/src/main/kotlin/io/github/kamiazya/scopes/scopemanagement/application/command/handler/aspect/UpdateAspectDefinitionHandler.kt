package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Handler for updating an existing aspect definition.
 * Currently only supports updating the description, as changing the type
 * could break existing data.
 */
class UpdateAspectDefinitionHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    CommandHandler<UpdateAspectDefinitionCommand, ScopeManagementApplicationError, AspectDefinition> {

    override suspend operator fun invoke(command: UpdateAspectDefinitionCommand): Either<ScopeManagementApplicationError, AspectDefinition> =
        transactionManager.inTransaction {
            either {
                // Validate and create aspect key
                val aspectKey = AspectKey.create(command.key)
                    .mapLeft { it.toGenericApplicationError() }
                    .bind()

                // Find existing definition
                val existing = aspectDefinitionRepository.findByKey(aspectKey).fold(
                    { _ ->
                        raise(
                            ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                operation = "retrieve-aspect-definition",
                            ),
                        )
                    },
                    { definition ->
                        definition ?: raise(
                            ScopeManagementApplicationError.PersistenceError.NotFound(
                                entityType = "AspectDefinition",
                                entityId = command.key,
                            ),
                        )
                    },
                )

                // Update only if description is provided and different
                if (command.description == null || command.description == existing.description) {
                    return@either existing
                }

                val updated = existing.copy(description = command.description)

                // Save updated definition
                aspectDefinitionRepository.save(updated).fold(
                    { _ ->
                        raise(
                            ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                operation = "update-aspect-definition",
                            ),
                        )
                    },
                    { saved -> saved },
                )
            }
        }
}
