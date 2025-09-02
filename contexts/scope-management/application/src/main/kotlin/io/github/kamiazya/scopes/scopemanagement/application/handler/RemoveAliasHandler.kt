package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for removing aliases from scopes.
 * Ensures canonical aliases cannot be removed.
 */
class RemoveAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<RemoveAlias, ScopesError, Unit> {

    override suspend operator fun invoke(input: RemoveAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Removing alias",
                mapOf("aliasName" to input.aliasName),
            )

            // Validate aliasName
            val aliasName = AliasName.create(input.aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid alias name",
                        mapOf(
                            "aliasName" to input.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.aliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.aliasName, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.aliasName, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.aliasName, error.expectedPattern)
                    }
                }
                .bind()

            // Find alias by name first
            val alias = scopeAliasService.findAliasByName(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find alias",
                        mapOf(
                            "aliasName" to input.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (alias == null) {
                logger.error(
                    "Alias not found",
                    mapOf("aliasName" to input.aliasName),
                )
                raise(ScopeInputError.AliasNotFound(input.aliasName))
            }

            // Remove alias through application service
            scopeAliasService.deleteAlias(alias.id)
                .mapLeft { error ->
                    logger.error(
                        "Failed to remove alias",
                        mapOf(
                            "aliasName" to input.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            logger.info(
                "Successfully removed alias",
                mapOf("aliasName" to input.aliasName),
            )
        }
    }
}
