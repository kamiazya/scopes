package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for removing aliases from scopes.
 * Ensures canonical aliases cannot be removed.
 */
class RemoveAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : CommandHandler<RemoveAliasCommand, ScopeManagementApplicationError, Unit> {

    private val errorPresenter = ScopeInputErrorPresenter()

    override suspend operator fun invoke(command: RemoveAliasCommand): Either<ScopeManagementApplicationError, Unit> = transactionManager.inTransaction {
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
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.EmptyAlias ->
                            ScopeInputError.AliasEmpty(command.aliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.AliasTooShort ->
                            ScopeInputError.AliasTooShort(command.aliasName, error.minLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.AliasTooLong ->
                            ScopeInputError.AliasTooLong(command.aliasName, error.maxLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidAliasFormat ->
                            ScopeInputError.AliasInvalidFormat(command.aliasName, errorPresenter.presentAliasPattern(error.expectedPattern))
                    }
                }
                .bind()

            // Find alias by name first
            val alias = scopeAliasService.findAliasByName(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error
                }
                .bind()

            if (alias == null) {
                logger.error(
                    "Alias not found",
                    mapOf("aliasName" to command.aliasName),
                )
                raise(ScopeInputError.AliasNotFound(command.aliasName))
            }

            // Remove alias through application service
            scopeAliasService.deleteAlias(alias.id)
                .mapLeft { error ->
                    logger.error(
                        "Failed to remove alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error
                }
                .bind()

            logger.info(
                "Successfully removed alias",
                mapOf("aliasName" to command.aliasName),
            )
        }
    }
}
