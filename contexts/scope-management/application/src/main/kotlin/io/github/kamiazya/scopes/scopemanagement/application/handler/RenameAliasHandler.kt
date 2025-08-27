package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for renaming existing aliases.
 * This operation preserves the alias type (canonical/custom) and other properties.
 */
class RenameAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
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

            // First, resolve the current alias to get the scope ID and verify it exists
            val scopeId = scopeAliasService.resolveAlias(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to resolve current alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    // Map to application error - alias not found becomes input error
                    ScopeInputError.AliasNotFound(input.currentAlias)
                }
                .bind()

            // Get the current alias to check if it's canonical
            val currentAlias = scopeAliasService.getAliasesForScope(scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Failed to get aliases for scope",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()
                .find { it.aliasName == currentAliasName }

            if (currentAlias == null) {
                logger.error(
                    "Current alias not found in scope",
                    mapOf("currentAlias" to input.currentAlias),
                )
                raise(ScopeInputError.AliasNotFound(input.currentAlias))
            }

            // Check if new alias name already exists
            val existingNewAlias = scopeAliasService.resolveAlias(newAliasName)
                .fold(
                    ifLeft = { null }, // Alias doesn't exist, which is what we want
                    ifRight = { existingScopeId ->
                        if (existingScopeId != scopeId) {
                            logger.error(
                                "New alias name already exists for different scope",
                                mapOf(
                                    "newAliasName" to input.newAliasName,
                                    "existingScopeId" to existingScopeId.value,
                                    "currentScopeId" to scopeId.value,
                                ),
                            )
                            raise(
                                ScopeAliasError.AliasDuplicate(
                                    aliasName = input.newAliasName,
                                    existingScopeId = existingScopeId.value,
                                    attemptedScopeId = scopeId.value,
                                ),
                            )
                        }
                        existingScopeId
                    },
                )

            // Remove the old alias first
            scopeAliasService.removeAlias(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to remove old alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            // Add the new alias with the same type (canonical or custom)
            val result = if (currentAlias.isCanonical()) {
                scopeAliasService.assignCanonicalAlias(scopeId, newAliasName)
            } else {
                scopeAliasService.assignCustomAlias(scopeId, newAliasName)
            }

            result.mapLeft { error ->
                logger.error(
                    "Failed to create new alias",
                    mapOf(
                        "newAliasName" to input.newAliasName,
                        "aliasType" to if (currentAlias.isCanonical()) "canonical" else "custom",
                        "error" to error.toString(),
                    ),
                )
                error.toApplicationError()
            }.bind()

            logger.info(
                "Successfully renamed alias",
                mapOf(
                    "currentAlias" to input.currentAlias,
                    "newAliasName" to input.newAliasName,
                    "scopeId" to scopeId.value,
                    "aliasType" to if (currentAlias.isCanonical()) "canonical" else "custom",
                ),
            )
        }
    }
}
