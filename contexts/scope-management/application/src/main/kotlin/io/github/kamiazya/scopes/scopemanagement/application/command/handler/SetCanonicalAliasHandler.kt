package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for setting a canonical alias for a scope.
 * Automatically demotes the previous canonical alias to a custom alias.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class SetCanonicalAliasHandler(private val scopeAliasService: ScopeAliasApplicationService, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<SetCanonicalAliasCommand, Unit>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: SetCanonicalAliasCommand): Either<ScopeManagementApplicationError, Unit> = either {
        logger.debug(
            "Setting canonical alias",
            mapOf(
                "currentAlias" to command.currentAlias,
                "newCanonicalAlias" to command.newCanonicalAlias,
            ),
        )

        // Validate and find aliases
        val currentAliasName = validateAliasName(command.currentAlias, "current").bind()
        val currentAlias = findAlias(currentAliasName, command.currentAlias).bind()
        val scopeId = currentAlias.scopeId

        val newCanonicalAliasName = validateAliasName(command.newCanonicalAlias, "new canonical").bind()
        val newCanonicalAlias = findAlias(newCanonicalAliasName, command.newCanonicalAlias).bind()

        // Verify aliases belong to same scope
        verifySameScope(currentAlias, newCanonicalAlias, command).bind()

        // Set as canonical
        setCanonicalAlias(scopeId, newCanonicalAliasName, command).bind()

        logger.info(
            "Successfully set canonical alias",
            mapOf(
                "currentAlias" to command.currentAlias,
                "newCanonicalAlias" to command.newCanonicalAlias,
                "scopeId" to scopeId.value,
            ),
        )
    }

    private fun validateAliasName(alias: String, aliasType: String): Either<ScopeManagementApplicationError, AliasName> =
        AliasName.create(alias).mapLeft { error ->
            logger.error(
                "Invalid $aliasType alias name",
                mapOf(
                    "${aliasType}Alias" to alias,
                    "error" to error.toString(),
                ),
            )
            mapAliasError(error, alias)
        }

    private fun mapAliasError(error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError, alias: String): ScopeInputError =
        errorMappingService.mapAliasError(error, alias)

    private suspend fun findAlias(aliasName: AliasName, aliasString: String): Either<ScopeManagementApplicationError, ScopeAlias> = either {
        val alias = scopeAliasService.findAliasByName(aliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find alias",
                    mapOf(
                        "alias" to aliasString,
                        "error" to error.toString(),
                    ),
                )
                error
            }
            .bind()

        if (alias == null) {
            logger.error(
                "Alias not found",
                mapOf("alias" to aliasString),
            )
            raise(ScopeInputError.AliasNotFound(aliasString))
        }

        alias
    }

    private fun verifySameScope(
        currentAlias: ScopeAlias,
        newCanonicalAlias: ScopeAlias,
        command: SetCanonicalAliasCommand,
    ): Either<ScopeManagementApplicationError, Unit> = either {
        if (newCanonicalAlias.scopeId != currentAlias.scopeId) {
            logger.error(
                "New canonical alias belongs to different scope",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
                    "currentAliasScope" to currentAlias.scopeId.value,
                    "newCanonicalAliasScope" to newCanonicalAlias.scopeId.value,
                ),
            )
            raise(
                ScopeInputError.AliasOfDifferentScope(
                    alias = command.newCanonicalAlias,
                    expectedScopeId = currentAlias.scopeId.value,
                    actualScopeId = newCanonicalAlias.scopeId.value,
                ),
            )
        }
    }

    private suspend fun setCanonicalAlias(
        scopeId: ScopeId,
        aliasName: AliasName,
        command: SetCanonicalAliasCommand,
    ): Either<ScopeManagementApplicationError, Unit> = scopeAliasService.assignCanonicalAlias(scopeId, aliasName)
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
            error
        }
        .map { Unit }
}
