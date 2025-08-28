package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for retrieving a scope by its alias name.
 * This supports both canonical and custom aliases.
 */
class GetScopeByAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<GetScopeByAliasQuery, ScopesError, ScopeDto> {

    override suspend operator fun invoke(input: GetScopeByAliasQuery): Either<ScopesError, ScopeDto> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Getting scope by alias",
                mapOf("aliasName" to input.aliasName),
            )

            // Validate alias name
            val aliasName = AliasName.create(input.aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid alias name",
                        mapOf(
                            "aliasName" to input.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.aliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.aliasName, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.aliasName, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.aliasName, error.expectedPattern)
                    }
                }
                .bind()

            // Resolve alias to scope ID through domain service
            val scopeId = scopeAliasService.resolveAlias(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to resolve alias",
                        mapOf(
                            "aliasName" to input.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    // Map domain error to appropriate application error
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError.AliasNotFound ->
                            ScopeInputError.AliasNotFound(input.aliasName)
                        else ->
                            // For other errors (like persistence errors), use the generic error mapping
                            error.toGenericApplicationError()
                    }
                }
                .bind()

            // Get scope by ID
            val scope = scopeRepository.findById(scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find scope",
                        mapOf(
                            "scopeId" to scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (scope == null) {
                logger.warn(
                    "Scope not found for alias",
                    mapOf(
                        "aliasName" to input.aliasName,
                        "scopeId" to scopeId.value,
                    ),
                )
                // This is an inconsistency - alias exists but scope doesn't
                raise(ScopeAliasError.AliasNotFound(input.aliasName))
            }

            // Get all aliases for the scope to include in response through domain service
            val aliases = scopeAliasService.getAliasesForScope(scope.id)
                .mapLeft { error ->
                    logger.warn(
                        "Failed to get aliases for scope",
                        mapOf(
                            "scopeId" to scope.id.value,
                            "error" to error.toString(),
                        ),
                    )
                    // Continue with empty list on failure
                    error.toGenericApplicationError()
                }
                .getOrElse { emptyList() }

            logger.info(
                "Successfully retrieved scope by alias",
                mapOf(
                    "aliasName" to input.aliasName,
                    "scopeId" to scope.id.value,
                ),
            )

            val canonicalAlias = aliases.find { it.isCanonical() }?.aliasName?.value
            val customAliases = aliases.filter { !it.isCanonical() }.map { it.aliasName.value }

            ScopeMapper.toDto(scope, canonicalAlias, customAliases)
        }
    }
}
