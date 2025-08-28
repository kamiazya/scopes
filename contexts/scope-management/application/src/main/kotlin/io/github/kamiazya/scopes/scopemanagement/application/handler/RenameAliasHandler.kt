package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for renaming existing aliases.
 * This operation is atomic and preserves the alias type (canonical/custom).
 * The rename is performed in a single transaction to prevent data loss.
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

            // Perform atomic rename operation
            // This method handles all validation and ensures atomicity:
            // - Verifies the old alias exists
            // - Checks new alias availability
            // - Preserves alias type (canonical/custom)
            // - Performs rename in single transaction
            val renamedAlias = scopeAliasService.renameAlias(currentAliasName, newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to rename alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "newAlias" to input.newAliasName,
                            "error" to error.toString(),
                        ),
                    )
                    // Map specific domain errors to application errors
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError.AliasNotFound -> {
                            // The old alias doesn't exist
                            ScopeInputError.AliasNotFound(input.currentAlias)
                        }
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError.DuplicateAlias -> {
                            // The new alias already exists for another scope
                            io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError.AliasDuplicate(
                                aliasName = input.newAliasName,
                                existingScopeId = error.existingScopeId.value,
                                attemptedScopeId = error.attemptedScopeId.value,
                            )
                        }
                        else -> {
                            // For other errors (like persistence errors), use the generic error mapping
                            error.toApplicationError()
                        }
                    }
                }
                .bind()

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
