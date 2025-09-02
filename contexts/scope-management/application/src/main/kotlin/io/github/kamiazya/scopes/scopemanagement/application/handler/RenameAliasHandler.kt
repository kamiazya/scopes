package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
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
) : UseCase<RenameAlias, ScopesError, Unit> {

    override suspend operator fun invoke(input: RenameAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Renaming alias",
                mapOf(
                    "currentAlias" to input.currentAlias,
                    "newAliasName" to input.newAliasName,
                ),
            )

            // Validate currentAlias
            val currentAliasName = AliasName.create(input.currentAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid current alias name",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.currentAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.currentAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.currentAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.currentAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Validate newAliasName
            val newAliasName = AliasName.create(input.newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new alias name",
                        mapOf(
                            "newAliasName" to input.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.newAliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.newAliasName, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.newAliasName, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.newAliasName, error.expectedPattern)
                    }
                }
                .bind()

            // Find the current alias
            val currentAlias = scopeAliasService.findAliasByName(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find current alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (currentAlias == null) {
                logger.error(
                    "Current alias not found",
                    mapOf("currentAlias" to input.currentAlias),
                )
                raise(ScopeInputError.AliasNotFound(input.currentAlias))
            }

            // Check if new alias name is already taken
            val existingNewAlias = scopeAliasService.findAliasByName(newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to check new alias availability",
                        mapOf(
                            "newAlias" to input.newAliasName,
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
                        "newAlias" to input.newAliasName,
                        "existingScopeId" to existingNewAlias.scopeId.value,
                        "attemptedScopeId" to currentAlias.scopeId.value,
                    ),
                )
                raise(
                    io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError.AliasDuplicate(
                        aliasName = input.newAliasName,
                        existingScopeId = existingNewAlias.scopeId.value,
                        attemptedScopeId = currentAlias.scopeId.value,
                    )
                )
            }

            // Delete the old alias
            scopeAliasService.deleteAlias(currentAlias.id)
                .mapLeft { error ->
                    logger.error(
                        "Failed to delete old alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            // Create new alias with the same type
            val renamedAlias = if (currentAlias.isCanonical()) {
                scopeAliasService.assignCanonicalAlias(currentAlias.scopeId, newAliasName)
                    .mapLeft { error ->
                        logger.error(
                            "Failed to create new canonical alias",
                            mapOf(
                                "newAlias" to input.newAliasName,
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
                                "newAlias" to input.newAliasName,
                                "scopeId" to currentAlias.scopeId.value,
                                "error" to error.toString(),
                            ),
                        )
                        error.toGenericApplicationError()
                    }
                    .bind()
            }

            logger.info(
                "Successfully renamed alias",
                mapOf(
                    "currentAlias" to input.currentAlias,
                    "newAliasName" to input.newAliasName,
                    "scopeId" to renamedAlias.scopeId.value,
                    "aliasType" to if (renamedAlias.isCanonical()) "canonical" else "custom",
                ),
            )
        }
    }
}
