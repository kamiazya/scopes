package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for adding custom aliases to scopes.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class AddAliasHandler(private val scopeAliasService: ScopeAliasApplicationService, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<AddAliasCommand, Unit>(transactionManager, logger) {

    private val errorPresenter = ScopeInputErrorPresenter()
    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: AddAliasCommand): Either<ScopeManagementApplicationError, Unit> = either {
        // Validate existingAlias
        val existingAliasName = AliasName.create(command.existingAlias)
            .mapLeft { error ->
                errorMappingService.mapAliasError(error, command.existingAlias)
            }
            .bind()

        // Find the scope ID through application service
        val alias = ensureNotNull(
            scopeAliasService.findAliasByName(existingAliasName).bind(),
        ) {
            ScopeInputError.AliasNotFound(command.existingAlias)
        }

        val scopeId = alias.scopeId

        // Validate newAlias
        val newAliasName = AliasName.create(command.newAlias)
            .mapLeft { error ->
                errorMappingService.mapAliasError(error, command.newAlias)
            }
            .bind()

        // Add alias through application service
        scopeAliasService.createCustomAlias(scopeId, newAliasName).bind()
    }
}
