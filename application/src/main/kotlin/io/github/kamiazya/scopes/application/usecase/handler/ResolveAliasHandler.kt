package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ResolveAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
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
                        ApplicationError.ScopeInputError.AliasEmpty(aliasError.attemptedValue)
                    is ScopeInputError.AliasError.TooShort ->
                        ApplicationError.ScopeInputError.AliasTooShort(aliasError.attemptedValue, aliasError.minimumLength)
                    is ScopeInputError.AliasError.TooLong ->
                        ApplicationError.ScopeInputError.AliasTooLong(aliasError.attemptedValue, aliasError.maximumLength)
                    is ScopeInputError.AliasError.InvalidFormat ->
                        ApplicationError.ScopeInputError.AliasInvalidFormat(aliasError.attemptedValue, aliasError.expectedPattern)
                }
            }
            .flatMap { aliasName ->
                aliasManagementService.resolveAlias(aliasName)
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
                    .map { scopeId -> ResolveAliasResult(scopeId.value) }
            }
    }
}

