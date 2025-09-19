package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for removing aliases from scopes.
 * Ensures canonical aliases cannot be removed.
 *
 * Note: This handler returns contract errors directly as part of the
 * architecture simplification to eliminate duplicate error definitions.
 */
class RemoveAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<RemoveAliasCommand, ScopeContractError, Unit> {

    override suspend operator fun invoke(command: RemoveAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
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
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = command.aliasName),
                    )
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
                    applicationErrorMapper.mapToContractError(error)
                }
                .bind()

            if (alias == null) {
                logger.error(
                    "Alias not found",
                    mapOf("aliasName" to command.aliasName),
                )
                raise(ScopeContractError.BusinessError.AliasNotFound(alias = command.aliasName))
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
                    applicationErrorMapper.mapToContractError(error)
                }
                .bind()

            logger.info(
                "Successfully removed alias",
                mapOf("aliasName" to command.aliasName),
            )
        }
    }
}
