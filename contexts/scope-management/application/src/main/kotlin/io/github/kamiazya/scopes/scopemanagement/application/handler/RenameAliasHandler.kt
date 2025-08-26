package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for renaming existing aliases.
 * This operation preserves the alias type (canonical/custom) and other properties.
 */
class RenameAliasHandler(private val aliasRepository: ScopeAliasRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    UseCase<RenameAlias, ScopesError, Unit> {

    override suspend operator fun invoke(command: RenameAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Renaming alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                ),
            )

            // Validate currentAlias
            val currentAliasName = AliasName.create(command.currentAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid current alias name",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    ScopeInputError.InvalidAlias(command.currentAlias)
                }
                .bind()

            // Validate newAliasName
            val newAliasName = AliasName.create(command.newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new alias name",
                        mapOf(
                            "newAliasName" to command.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    ScopeInputError.InvalidAlias(command.newAliasName)
                }
                .bind()

            // Find the current alias
            val currentAliasEntity = aliasRepository.findByAliasName(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find current alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (currentAliasEntity == null) {
                logger.error(
                    "Current alias not found",
                    mapOf("currentAlias" to command.currentAlias),
                )
                return@either ScopeInputError.AliasNotFound(command.currentAlias).left().bind()
            }

            // Check if new alias name already exists
            val existingAlias = aliasRepository.findByAliasName(newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to check existing alias",
                        mapOf(
                            "newAliasName" to command.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (existingAlias != null) {
                logger.error(
                    "New alias name already exists",
                    mapOf(
                        "newAliasName" to command.newAliasName,
                        "existingScopeId" to existingAlias.scopeId.value,
                    ),
                )
                return@either ScopeInputError.InvalidAlias(command.newAliasName).left().bind()
            }

            // Create renamed alias preserving all properties except the name
            val renamedAlias = when (currentAliasEntity.isCanonical()) {
                true -> ScopeAlias.createCanonical(currentAliasEntity.scopeId, newAliasName)
                false -> ScopeAlias.createCustom(currentAliasEntity.scopeId, newAliasName)
            }.copy(createdAt = currentAliasEntity.createdAt) // Preserve original creation time

            // Delete old alias and save new one
            aliasRepository.removeByAliasName(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to delete old alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            aliasRepository.save(renamedAlias)
                .mapLeft { error ->
                    logger.error(
                        "Failed to save renamed alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "newAliasName" to command.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully renamed alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                    "scopeId" to currentAliasEntity.scopeId.value,
                    "aliasType" to currentAliasEntity.aliasType.name,
                ),
            )
        }
    }
}
