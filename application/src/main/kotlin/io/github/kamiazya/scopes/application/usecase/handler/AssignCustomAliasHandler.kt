package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ScopeAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ScopeAliasError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.AssignCustomAlias
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for assigning a custom alias to a scope.
 *
 * Implements the UseCase interface following Clean Architecture principles.
 * Multiple custom aliases can be assigned to a single scope.
 */
class AssignCustomAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
) : UseCase<AssignCustomAlias, ApplicationError, ScopeAliasResult> {

    override suspend operator fun invoke(input: AssignCustomAlias): Either<ApplicationError, ScopeAliasResult> = transactionManager.inTransaction {
        ScopeId.create(input.scopeId)
            .mapLeft { idError ->
                when (idError) {
                    is ScopeInputError.IdError.Blank -> AppScopeInputError.IdBlank(idError.attemptedValue)
                    is ScopeInputError.IdError.InvalidFormat -> AppScopeInputError.IdInvalidFormat(
                        idError.attemptedValue,
                        "ULID",
                    )
                }
            }
            .flatMap { scopeId ->
                AliasName.create(input.aliasName)
                    .mapLeft { aliasError ->
                        when (aliasError) {
                            is ScopeInputError.AliasError.Empty ->
                                AppScopeInputError.AliasEmpty(aliasError.attemptedValue)
                            is ScopeInputError.AliasError.TooShort ->
                                AppScopeInputError.AliasTooShort(
                                    aliasError.attemptedValue,
                                    aliasError.minimumLength,
                                )
                            is ScopeInputError.AliasError.TooLong ->
                                AppScopeInputError.AliasTooLong(aliasError.attemptedValue, aliasError.maximumLength)
                            is ScopeInputError.AliasError.InvalidFormat ->
                                AppScopeInputError.AliasInvalidFormat(
                                    aliasError.attemptedValue,
                                    aliasError.expectedPattern,
                                )
                        }
                    }
                    .flatMap { aliasName ->
                        aliasManagementService.assignCustomAlias(scopeId, aliasName)
                            .mapLeft { aliasServiceError ->
                                when (aliasServiceError) {
                                    is DomainScopeAliasError.DuplicateAlias ->
                                        ScopeAliasError.DuplicateAlias(
                                            aliasServiceError.aliasName,
                                            aliasServiceError.existingScopeId.value,
                                            aliasServiceError.attemptedScopeId.value,
                                        )
                                    is DomainScopeAliasError.AliasNotFound ->
                                        ScopeAliasError.AliasNotFound(
                                            aliasServiceError.aliasName,
                                        )
                                    is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
                                        ScopeAliasError.CannotRemoveCanonicalAlias(
                                            aliasServiceError.scopeId.value,
                                            aliasServiceError.canonicalAlias,
                                        )
                                    is DomainScopeAliasError.CanonicalAliasAlreadyExists ->
                                        ScopeAliasError.DuplicateAlias(
                                            aliasServiceError.existingCanonicalAlias,
                                            aliasServiceError.scopeId.value,
                                            aliasServiceError.scopeId.value,
                                        )
                                }
                            }
                            .map { alias -> ScopeAliasMapper.toDto(alias) }
                    }
            }
    }
}
