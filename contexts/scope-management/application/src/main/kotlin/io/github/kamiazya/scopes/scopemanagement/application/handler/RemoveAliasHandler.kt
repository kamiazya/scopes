package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for removing aliases from scopes.
 * Ensures canonical aliases cannot be removed.
 */
class RemoveAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<RemoveAlias, ScopesError, Unit> {

    override suspend operator fun invoke(command: RemoveAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Removing alias",
                mapOf("aliasName" to command.aliasName),
            )

            // Validate aliasName
            val aliasName = AliasName.create(command.aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid alias name",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.aliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.aliasName, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.aliasName, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.aliasName, error.expectedPattern)
                    }
                }
                .bind()

            // Remove alias through domain service
            scopeAliasService.removeAlias(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to remove alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully removed alias",
                mapOf("aliasName" to command.aliasName),
            )
        }
    }
}
