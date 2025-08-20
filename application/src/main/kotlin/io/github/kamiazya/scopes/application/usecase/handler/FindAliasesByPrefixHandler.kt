package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import io.github.kamiazya.scopes.application.dto.ListAliasesResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ScopeAliasError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.query.FindAliasesByPrefixQuery
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for finding aliases that start with a given prefix.
 *
 * Used for tab completion and partial matching in CLI/UI interfaces.
 */
class FindAliasesByPrefixHandler(private val aliasManagementService: ScopeAliasManagementService) : UseCase<FindAliasesByPrefixQuery, ApplicationError, ListAliasesResult> {

    override suspend operator fun invoke(input: FindAliasesByPrefixQuery): Either<ApplicationError, ListAliasesResult> = aliasManagementService.findAliasesByPrefix(input.prefix, input.limit)
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
        .map { aliases -> ListAliasesResult(ScopeAliasMapper.toDtoList(aliases)) }
}
