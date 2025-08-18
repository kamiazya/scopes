package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.RemoveAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.RemoveAlias
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName

/**
 * Handler for removing an alias.
 *
 * Business rules:
 * - Cannot remove canonical aliases (must replace instead)
 * - Custom aliases can be removed freely
 */
class RemoveAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager
) : UseCase<RemoveAlias, ApplicationError, RemoveAliasResult> {

    override suspend operator fun invoke(input: RemoveAlias): Either<ApplicationError, RemoveAliasResult> = either {
        transactionManager.inTransaction {
            either {
                val aliasName = AliasName.create(input.aliasName)
                    .mapLeft { aliasError ->
                        when(aliasError) {
                            is ScopeInputError.AliasError.Empty ->
                                ApplicationError.ScopeInputError.AliasEmpty(aliasError.attemptedValue)
                            is ScopeInputError.AliasError.TooShort ->
                                ApplicationError.ScopeInputError.AliasTooShort(aliasError.attemptedValue, aliasError.minimumLength)
                            is ScopeInputError.AliasError.TooLong ->
                                ApplicationError.ScopeInputError.AliasTooLong(aliasError.attemptedValue, aliasError.maximumLength)
                            is ScopeInputError.AliasError.InvalidFormat ->
                                ApplicationError.ScopeInputError.AliasInvalidFormat(aliasError.attemptedValue, aliasError.expectedPattern)
                        }
                    }
                    .bind()

                val removedAlias = aliasManagementService.removeAlias(aliasName)
                    .mapLeft { aliasServiceError ->
                        when (aliasServiceError) {
                            is DomainScopeAliasError.DuplicateAlias ->
                                ApplicationError.ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.aliasName,
                                    aliasServiceError.existingScopeId.value,
                                    aliasServiceError.attemptedScopeId.value
                                )
                            is DomainScopeAliasError.AliasNotFound ->
                                ApplicationError.ScopeAliasError.AliasNotFound(
                                    aliasServiceError.aliasName
                                )
                            is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
                                ApplicationError.ScopeAliasError.CannotRemoveCanonicalAlias(
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.canonicalAlias
                                )
                            is DomainScopeAliasError.CanonicalAliasAlreadyExists ->
                                ApplicationError.ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.existingCanonicalAlias,
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.scopeId.value
                                )
                        }
                    }
                    .bind()

                RemoveAliasResult(
                    aliasId = removedAlias.id.value,
                    aliasName = removedAlias.aliasName.value,
                    scopeId = removedAlias.scopeId.value,
                    wasCanonical = removedAlias.isCanonical()
                )
            }
        }.bind()
    }
}
