package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ResolveAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ScopeAliasError
import io.github.kamiazya.scopes.application.error.ScopeInputError as AppScopeInputError
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.query.ResolveAliasQuery
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for resolving an alias to a scope ID.
 *
 * This query handler translates human-readable aliases to internal scope IDs,
 * abstracting away the ULID implementation details from users.
 */
class ResolveAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService
) : UseCase<ResolveAliasQuery, ApplicationError, ResolveAliasResult> {

    override suspend operator fun invoke(input: ResolveAliasQuery): Either<ApplicationError, ResolveAliasResult> {
        return AliasName.create(input.aliasName)
            .mapLeft { aliasError ->
                when(aliasError) {
                    is ScopeInputError.AliasError.Empty ->
                        AppScopeInputError.AliasEmpty(aliasError.attemptedValue)
                    is ScopeInputError.AliasError.TooShort ->
                        AppScopeInputError.AliasTooShort(aliasError.attemptedValue, aliasError.minimumLength)
                    is ScopeInputError.AliasError.TooLong ->
                        AppScopeInputError.AliasTooLong(aliasError.attemptedValue, aliasError.maximumLength)
                    is ScopeInputError.AliasError.InvalidFormat ->
                        AppScopeInputError.AliasInvalidFormat(aliasError.attemptedValue, aliasError.expectedPattern)
                }
            }
            .flatMap { aliasName ->
                aliasManagementService.resolveAlias(aliasName)
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
                    .map { scopeId -> ResolveAliasResult(scopeId.value) }
            }
    }
}

