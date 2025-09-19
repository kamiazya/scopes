package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for setting a canonical alias for a scope.
 * Automatically demotes the previous canonical alias to a custom alias.
 *
 * Note: This handler returns contract errors directly as part of the
 * architecture simplification to eliminate duplicate error definitions.
 */
class SetCanonicalAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<SetCanonicalAliasCommand, ScopeContractError, Unit> {

    override suspend operator fun invoke(command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        either {
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
    }

    private fun validateAliasName(alias: String, aliasType: String): Either<ScopeContractError, AliasName> = AliasName.create(alias).mapLeft { error ->
        logger.error(
            "Invalid $aliasType alias name",
            mapOf(
                "${aliasType}Alias" to alias,
                "error" to error.toString(),
            ),
        )
        applicationErrorMapper.mapDomainError(
            error,
            ErrorMappingContext(attemptedValue = alias),
        )
    }

    private suspend fun findAlias(aliasName: AliasName, aliasString: String): Either<ScopeContractError, ScopeAlias> = either {
        val alias = scopeAliasService.findAliasByName(aliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find alias",
                    mapOf(
                        "alias" to aliasString,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapToContractError(error)
            }
            .bind()

        if (alias == null) {
            logger.error(
                "Alias not found",
                mapOf("alias" to aliasString),
            )
            raise(ScopeContractError.BusinessError.AliasNotFound(alias = aliasString))
        }

        alias
    }

    private fun verifySameScope(currentAlias: ScopeAlias, newCanonicalAlias: ScopeAlias, command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> =
        either {
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
                    ScopeContractError.BusinessError.AliasOfDifferentScope(
                        alias = command.newCanonicalAlias,
                        expectedScopeId = currentAlias.scopeId.value,
                        actualScopeId = newCanonicalAlias.scopeId.value,
                    ),
                )
            }
        }

    private suspend fun setCanonicalAlias(scopeId: ScopeId, aliasName: AliasName, command: SetCanonicalAliasCommand): Either<ScopeContractError, Unit> =
        scopeAliasService.assignCanonicalAlias(scopeId, aliasName)
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
                applicationErrorMapper.mapToContractError(error)
            }
            .map { Unit }
}
