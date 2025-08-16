package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ScopeAliasDTO
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.GenerateCanonicalAlias
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.ScopeId

/**
 * Handler for generating and assigning a canonical alias using Haikunator pattern.
 * 
 * This handler generates a human-readable alias automatically based on
 * the alias's internal ID, making it self-contained and deterministic.
 */
class GenerateCanonicalAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager
) : UseCase<GenerateCanonicalAlias, ApplicationError, ScopeAliasDTO> {

    override suspend operator fun invoke(input: GenerateCanonicalAlias): Either<ApplicationError, ScopeAliasDTO> {
        return transactionManager.inTransaction {
            ScopeId.create(input.scopeId)
                .mapLeft { idError -> 
                    when(idError) {
                        is ScopeInputError.IdError.Blank -> ApplicationError.ScopeInputError.IdBlank(idError.attemptedValue)
                        is ScopeInputError.IdError.InvalidFormat -> ApplicationError.ScopeInputError.IdInvalidFormat(idError.attemptedValue, "ULID")
                    }
                }
                .flatMap { scopeId ->
                    aliasManagementService.generateCanonicalAlias(scopeId)
                        .mapLeft { aliasServiceError -> 
                            when (aliasServiceError) {
                                is DomainScopeAliasError.DuplicateAlias -> 
                                    ApplicationError.ScopeAliasError.DuplicateAlias(
                                        aliasServiceError.aliasName,
                                        aliasServiceError.existingScopeId.value,
                                        aliasServiceError.attemptedScopeId.value
                                    )
                                is DomainScopeAliasError.AliasNotFound -> 
                                    ApplicationError.ScopeAliasError.AliasGenerationFailed(
                                        scopeId.value,
                                        1  // Retry count, we could enhance this later
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
                        .map { alias -> ScopeAliasMapper.toDto(alias) }
                }
        }
    }
}