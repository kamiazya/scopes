package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ListAliasesResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ScopeAliasError
import io.github.kamiazya.scopes.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.query.ListAliasesForScopeQuery
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for listing all aliases assigned to a specific scope.
 *
 * Returns both canonical and custom aliases for the scope.
 */
class ListAliasesForScopeHandler(
    private val aliasManagementService: ScopeAliasManagementService
) : UseCase<ListAliasesForScopeQuery, ApplicationError, ListAliasesResult> {

    override suspend operator fun invoke(input: ListAliasesForScopeQuery): Either<ApplicationError, ListAliasesResult> {
        return ScopeId.create(input.scopeId)
            .mapLeft { idError ->
                when(idError) {
                    is ScopeInputError.IdError.Blank -> AppScopeInputError.IdBlank(idError.attemptedValue)
                    is ScopeInputError.IdError.InvalidFormat -> AppScopeInputError.IdInvalidFormat(idError.attemptedValue, "ULID")
                }
            }
            .flatMap { scopeId ->
                aliasManagementService.getAliasesForScope(scopeId)
                    .mapLeft { aliasServiceError ->
                        when (aliasServiceError) {
                            is DomainScopeAliasError.DuplicateAlias ->
                                ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.aliasName,
                                    aliasServiceError.existingScopeId.value,
                                    aliasServiceError.attemptedScopeId.value
                                )
                            is DomainScopeAliasError.AliasNotFound ->
                                ScopeAliasError.AliasNotFound(
                                    aliasServiceError.aliasName
                                )
                            is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
                                ScopeAliasError.CannotRemoveCanonicalAlias(
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.canonicalAlias
                                )
                            is DomainScopeAliasError.CanonicalAliasAlreadyExists ->
                                ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.existingCanonicalAlias,
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.scopeId.value
                                )
                        }
                    }
                    .map { aliases -> ListAliasesResult(ScopeAliasMapper.toDtoList(aliases)) }
            }
    }
}

