package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for adding custom aliases to scopes.
 */
class AddAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<AddAlias, ScopesError, Unit> {

    override suspend operator fun invoke(command: AddAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Adding alias to scope",
                mapOf(
                    "aliasName" to command.aliasName,
                    "scopeId" to command.scopeId,
                ),
            )

            // Validate scopeId
            val scopeId = ScopeId.create(command.scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Invalid scope ID",
                        mapOf(
                            "scopeId" to command.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    ScopeInputError.IdInvalidFormat(command.scopeId, "ULID")
                }
                .bind()

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
                    ScopeInputError.InvalidAlias(command.aliasName)
                }
                .bind()

            // Add alias through domain service
            scopeAliasService.assignCustomAlias(scopeId, aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to add alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "scopeId" to command.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully added alias",
                mapOf(
                    "aliasName" to command.aliasName,
                    "scopeId" to command.scopeId,
                ),
            )
        }
    }
}
