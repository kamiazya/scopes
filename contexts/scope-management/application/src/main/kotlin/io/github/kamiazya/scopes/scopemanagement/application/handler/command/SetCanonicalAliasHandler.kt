package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.SetCanonicalAliasCommand
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

            val currentAliasName = validateAndParseAliasName(command.currentAlias, "current").bind()
            val currentAlias = findExistingAlias(currentAliasName, command.currentAlias, "current").bind()
            val scopeId = currentAlias.scopeId

            val newCanonicalAliasName = validateAndParseAliasName(command.newCanonicalAlias, "new canonical").bind()
            val newCanonicalAlias = findExistingAlias(newCanonicalAliasName, command.newCanonicalAlias, "new canonical").bind()
            
            validateSameScopeAlias(scopeId, newCanonicalAlias.scopeId, command).bind()
            assignCanonicalAlias(scopeId, newCanonicalAliasName, command).bind()

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

    private fun validateAndParseAliasName(aliasName: String, aliasType: String): Either<ScopesError, AliasName> {
        return AliasName.create(aliasName)
            .mapLeft { error ->
                logger.error(
                    "Invalid $aliasType alias name",
                    mapOf(
                        "aliasName" to aliasName,
                        "aliasType" to aliasType,
                        "error" to error.toString(),
                    ),
                )
                mapAliasError(error, aliasName)
            }
    }

    private fun mapAliasError(error: io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError, aliasName: String): ScopeInputError {
        return when (error) {
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                ScopeInputError.AliasEmpty(aliasName)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                ScopeInputError.AliasTooShort(aliasName, error.minimumLength)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                ScopeInputError.AliasTooLong(aliasName, error.maximumLength)
            is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                ScopeInputError.AliasInvalidFormat(aliasName, errorPresenter.presentAliasPattern(error.patternType))
        }
    }

    private suspend fun findExistingAlias(aliasName: AliasName, originalName: String, aliasType: String): Either<ScopesError, io.github.kamiazya.scopes.scopemanagement.domain.entity.Alias> = either {
        val alias = scopeAliasService.findAliasByName(aliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find $aliasType alias",
                    mapOf(
                        "aliasName" to originalName,
                        "aliasType" to aliasType,
                        "error" to error.toString(),
                    ),
                )
                error.toGenericApplicationError()
            }
            .bind()

        if (alias == null) {
            logger.error(
                "$aliasType alias not found",
                mapOf("aliasName" to originalName, "aliasType" to aliasType),
            )
            raise(ScopeInputError.AliasNotFound(originalName))
        }
        alias
    }

    private fun validateSameScopeAlias(
        currentScopeId: io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId,
        newScopeId: io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId,
        command: SetCanonicalAliasCommand
    ): Either<ScopesError, Unit> {
        return if (newScopeId != currentScopeId) {
            logger.error(
                "New canonical alias belongs to different scope",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
                    "currentAliasScope" to currentScopeId.value,
                    "newCanonicalAliasScope" to newScopeId.value,
                ),
            )
            ScopeInputError.InvalidAlias(command.newCanonicalAlias).left()
        } else {
            Unit.right()
        }
    }

    private suspend fun assignCanonicalAlias(
        scopeId: io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId,
        newCanonicalAliasName: AliasName,
        command: SetCanonicalAliasCommand
    ): Either<ScopesError, Unit> {
        return scopeAliasService.assignCanonicalAlias(scopeId, newCanonicalAliasName)
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
    }
}
