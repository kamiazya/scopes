package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for setting a canonical alias for a scope.
 * Automatically demotes the previous canonical alias to a custom alias.
 */
class SetCanonicalAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<SetCanonicalAlias, ScopesError, Unit> {

    override suspend operator fun invoke(input: SetCanonicalAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Setting canonical alias",
                mapOf(
                    "currentAlias" to input.currentAlias,
                    "newCanonicalAlias" to input.newCanonicalAlias,
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

            // Resolve current alias to get scope ID through domain service
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

            // Validate newCanonicalAlias
            val newCanonicalAliasName = AliasName.create(input.newCanonicalAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new canonical alias name",
                        mapOf(
                            "newCanonicalAlias" to input.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.newCanonicalAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.newCanonicalAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.newCanonicalAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.newCanonicalAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Resolve new canonical alias to verify it exists and get its scope ID
            val newCanonicalAliasScopeId = scopeAliasService.resolveAlias(newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to resolve new canonical alias",
                        mapOf(
                            "newCanonicalAlias" to input.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    // Map to application error - alias not found becomes input error
                    ScopeInputError.AliasNotFound(input.newCanonicalAlias)
                }
                .bind()

            // Verify the new canonical alias belongs to the same scope
            if (newCanonicalAliasScopeId != scopeId) {
                logger.error(
                    "New canonical alias belongs to different scope",
                    mapOf(
                        "currentAlias" to input.currentAlias,
                        "newCanonicalAlias" to input.newCanonicalAlias,
                        "currentAliasScope" to scopeId.value,
                        "newCanonicalAliasScope" to newCanonicalAliasScopeId.value,
                    ),
                )
                raise(ScopeInputError.InvalidAlias(input.newCanonicalAlias))
            }

            // Set as canonical (service handles the demotion logic and idempotency)
            scopeAliasService.assignCanonicalAlias(scopeId, newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to set canonical alias",
                        mapOf(
                            "currentAlias" to input.currentAlias,
                            "newCanonicalAlias" to input.newCanonicalAlias,
                            "scopeId" to scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully set canonical alias",
                mapOf(
                    "currentAlias" to input.currentAlias,
                    "newCanonicalAlias" to input.newCanonicalAlias,
                    "scopeId" to scopeId.value,
                ),
            )
        }
    }
}
