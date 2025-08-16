package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import io.github.kamiazya.scopes.application.dto.ListAliasesResult
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.query.FindAliasesByPrefix
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService

/**
 * Handler for finding aliases that start with a given prefix.
 * 
 * Used for tab completion and partial matching in CLI/UI interfaces.
 */
class FindAliasesByPrefixHandler(
    private val aliasManagementService: ScopeAliasManagementService
) : UseCase<FindAliasesByPrefix, ApplicationError, ListAliasesResult> {

    override suspend operator fun invoke(input: FindAliasesByPrefix): Either<ApplicationError, ListAliasesResult> {
        return aliasManagementService.findAliasesByPrefix(input.prefix, input.limit)
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
            .map { aliases -> ListAliasesResult(ScopeAliasMapper.toDtoList(aliases)) }
    }
}