package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RemoveAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for removing aliases from scopes.
 * Ensures canonical aliases cannot be removed.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class RemoveAliasHandler(private val scopeAliasService: ScopeAliasApplicationService, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<RemoveAliasCommand, Unit>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: RemoveAliasCommand): Either<ScopeManagementApplicationError, Unit> = either {
        // Validate aliasName
        val aliasName = AliasName.create(command.aliasName)
            .mapLeft { error ->
                errorMappingService.mapAliasError(error, command.aliasName)
            }
            .bind()

        // Find alias by name first
        val alias = scopeAliasService.findAliasByName(aliasName).bind()

        if (alias == null) {
            raise(ScopeInputError.AliasNotFound(command.aliasName))
        }

        // Remove alias through application service
        scopeAliasService.deleteAlias(alias.id).bind()
    }
}
