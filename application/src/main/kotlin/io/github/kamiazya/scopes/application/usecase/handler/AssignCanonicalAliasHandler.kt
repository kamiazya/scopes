package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.flatMap
import io.github.kamiazya.scopes.application.dto.ScopeAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.AssignCanonicalAlias
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for assigning a canonical alias to a scope.
 *
 * Implements the UseCase interface following Clean Architecture principles.
 * Each handler has a single responsibility and a single invoke method.
 */
class AssignCanonicalAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager
) : UseCase<AssignCanonicalAlias, ApplicationError, ScopeAliasResult> {

    override suspend operator fun invoke(input: AssignCanonicalAlias): Either<ApplicationError, ScopeAliasResult> {
        return transactionManager.inTransaction {
            ScopeId.create(input.scopeId)
                .mapLeft { idError ->
                    when(idError) {
                        is ScopeInputError.IdError.Blank -> ApplicationError.ScopeInputError.IdBlank(idError.attemptedValue)
                        is ScopeInputError.IdError.InvalidFormat -> ApplicationError.ScopeInputError.IdInvalidFormat(idError.attemptedValue, "ULID")
                    }
                }
                .flatMap { scopeId ->
                    AliasName.create(input.aliasName)
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
                            aliasManagementService.assignCanonicalAlias(scopeId, aliasName)
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
                                .map { alias -> ScopeAliasMapper.toDto(alias) }
                        }
                }
        }
    }
}
