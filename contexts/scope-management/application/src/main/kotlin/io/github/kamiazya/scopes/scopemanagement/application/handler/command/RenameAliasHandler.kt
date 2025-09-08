package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for renaming existing aliases.
 * This operation is atomic and preserves the alias type (canonical/custom).
 * The rename is performed in a single transaction to prevent data loss.
 */
class RenameAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : CommandHandler<RenameAliasCommand, ScopesError, Unit> {

    private val errorPresenter = ScopeInputErrorPresenter()

    override suspend operator fun invoke(command: RenameAliasCommand): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Renaming alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                ),
            )

            // Validate input parameters
            val currentAliasName = validateAliasName(command.currentAlias, "currentAlias").bind()
            val newAliasName = validateAliasName(command.newAliasName, "newAliasName").bind()

            // Find the current alias
            val currentAlias = findCurrentAlias(currentAliasName, command.currentAlias).bind()

            // Perform the atomic rename operation
            performAtomicRename(currentAlias, newAliasName, command).bind()

            logger.info(
                "Successfully renamed alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                    "scopeId" to currentAlias.scopeId.value,
                ),
            )
        }
    }

    private suspend fun validateAliasName(aliasName: String, fieldName: String): Either<ScopesError, AliasName> = AliasName.create(aliasName).mapLeft { error ->
        logger.error(
            "Invalid alias name",
            mapOf(
                "fieldName" to fieldName,
                "aliasName" to aliasName,
                "error" to error.toString(),
            ),
        )
        when (error) {
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                ScopeInputError.AliasEmpty(aliasName)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                ScopeInputError.AliasTooShort(aliasName, error.minimumLength)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                ScopeInputError.AliasTooLong(aliasName, error.maximumLength)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                ScopeInputError.AliasInvalidFormat(aliasName, errorPresenter.presentAliasPattern(error.patternType))
        }
    }

    private suspend fun findCurrentAlias(currentAliasName: AliasName, inputAlias: String): Either<ScopesError, ScopeAlias> = either {
        val currentAlias = scopeAliasService.findAliasByName(currentAliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find current alias",
                    mapOf(
                        "currentAlias" to inputAlias,
                        "error" to error.toString(),
                    ),
                )
                error.toGenericApplicationError()
            }
            .bind()

        if (currentAlias == null) {
            logger.error(
                "Current alias not found",
                mapOf("currentAlias" to inputAlias),
            )
            raise(ScopeInputError.AliasNotFound(inputAlias))
        }

        currentAlias
    }

    private suspend fun performAtomicRename(currentAlias: ScopeAlias, newAliasName: AliasName, command: RenameAliasCommand): Either<ScopesError, Unit> =
        either {
            // Check if new alias name is already taken
            val existingNewAlias = scopeAliasService.findAliasByName(newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to check new alias availability",
                        mapOf(
                            "newAlias" to command.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (existingNewAlias != null) {
                logger.error(
                    "New alias already exists",
                    mapOf(
                        "newAlias" to command.newAliasName,
                        "existingScopeId" to existingNewAlias.scopeId.value,
                        "attemptedScopeId" to currentAlias.scopeId.value,
                    ),
                )
                raise(
                    io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError.AliasDuplicate(
                        aliasName = command.newAliasName,
                        existingScopeId = existingNewAlias.scopeId.value,
                        attemptedScopeId = currentAlias.scopeId.value,
                    ),
                )
            }

            // Delete the old alias and create new one with same type
            scopeAliasService.deleteAlias(currentAlias.id)
                .mapLeft { error ->
                    logger.error(
                        "Failed to delete old alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            // Create new alias preserving the type
            if (currentAlias.isCanonical()) {
                scopeAliasService.assignCanonicalAlias(currentAlias.scopeId, newAliasName)
                    .mapLeft { error ->
                        logger.error(
                            "Failed to create new canonical alias",
                            mapOf(
                                "newAlias" to command.newAliasName,
                                "scopeId" to currentAlias.scopeId.value,
                                "error" to error.toString(),
                            ),
                        )
                        error.toGenericApplicationError()
                    }
                    .bind()
            } else {
                scopeAliasService.createCustomAlias(currentAlias.scopeId, newAliasName)
                    .mapLeft { error ->
                        logger.error(
                            "Failed to create new custom alias",
                            mapOf(
                                "newAlias" to command.newAliasName,
                                "scopeId" to currentAlias.scopeId.value,
                                "error" to error.toString(),
                            ),
                        )
                        error.toGenericApplicationError()
                    }
                    .bind()
            }
        }
}
