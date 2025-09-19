package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for renaming existing aliases.
 * This operation is atomic and preserves the alias type (canonical/custom).
 * The rename is performed in a single transaction to prevent data loss.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class RenameAliasHandler(private val scopeAliasService: ScopeAliasApplicationService, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<RenameAliasCommand, Unit>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: RenameAliasCommand): Either<ScopeManagementApplicationError, Unit> = either {
        // Validate input parameters
        val currentAliasName = AliasName.create(command.currentAlias)
            .mapLeft { errorMappingService.mapAliasError(it, command.currentAlias) }
            .bind()
        val newAliasName = AliasName.create(command.newAliasName)
            .mapLeft { errorMappingService.mapAliasError(it, command.newAliasName) }
            .bind()

        // Find the current alias
        val currentAlias = scopeAliasService.findAliasByName(currentAliasName).bind()
            ?: raise(ScopeInputError.AliasNotFound(command.currentAlias))

        // Check if new alias name is already taken
        val existingNewAlias = scopeAliasService.findAliasByName(newAliasName).bind()

        if (existingNewAlias != null) {
            raise(
                io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError.AliasDuplicate(
                    aliasName = command.newAliasName,
                    existingScopeId = existingNewAlias.scopeId.value,
                    attemptedScopeId = currentAlias.scopeId.value,
                ),
            )
        }

        // Delete the old alias and create new one with same type
        scopeAliasService.deleteAlias(currentAlias.id).bind()

        // Create new alias preserving the type
        if (currentAlias.isCanonical()) {
            scopeAliasService.assignCanonicalAlias(currentAlias.scopeId, newAliasName).bind()
        } else {
            scopeAliasService.createCustomAlias(currentAlias.scopeId, newAliasName).bind()
        }
    }
}
