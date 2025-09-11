package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.scope.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for setting a canonical alias for a scope.
 * Automatically demotes the previous canonical alias to a custom alias.
 */
class SetCanonicalAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : CommandHandler<SetCanonicalAliasCommand, ScopesError, Unit> {

    private val errorPresenter = ScopeInputErrorPresenter()

    override suspend operator fun invoke(command: SetCanonicalAliasCommand): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Setting canonical alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
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
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.currentAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.currentAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.currentAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.currentAlias, errorPresenter.presentAliasPattern(error.patternType))
                    }
                }
                .bind()

            // Find current alias through application service
            val currentAlias = scopeAliasService.findAliasByName(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find current alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (currentAlias == null) {
                logger.error(
                    "Current alias not found",
                    mapOf("currentAlias" to command.currentAlias),
                )
                raise(ScopeInputError.AliasNotFound(command.currentAlias))
            }

            val scopeId = currentAlias.scopeId

            // Validate newCanonicalAlias
            val newCanonicalAliasName = AliasName.create(command.newCanonicalAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new canonical alias name",
                        mapOf(
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.newCanonicalAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.newCanonicalAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.newCanonicalAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.newCanonicalAlias, errorPresenter.presentAliasPattern(error.patternType))
                    }
                }
                .bind()

            // Find new canonical alias to verify it exists and get its scope ID
            val newCanonicalAlias = scopeAliasService.findAliasByName(newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find new canonical alias",
                        mapOf(
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (newCanonicalAlias == null) {
                logger.error(
                    "New canonical alias not found",
                    mapOf("newCanonicalAlias" to command.newCanonicalAlias),
                )
                raise(ScopeInputError.AliasNotFound(command.newCanonicalAlias))
            }

            val newCanonicalAliasScopeId = newCanonicalAlias.scopeId

            // Verify the new canonical alias belongs to the same scope
            if (newCanonicalAliasScopeId != scopeId) {
                logger.error(
                    "New canonical alias belongs to different scope",
                    mapOf(
                        "currentAlias" to command.currentAlias,
                        "newCanonicalAlias" to command.newCanonicalAlias,
                        "currentAliasScope" to scopeId.value,
                        "newCanonicalAliasScope" to newCanonicalAliasScopeId.value,
                    ),
                )
                raise(ScopeInputError.InvalidAlias(command.newCanonicalAlias))
            }

            // Set as canonical (service handles the demotion logic and idempotency)
            scopeAliasService.assignCanonicalAlias(scopeId, newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to set canonical alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "scopeId" to scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            logger.info(
                "Successfully set canonical alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
                    "scopeId" to scopeId.value,
                ),
            )
        }
    }
}
