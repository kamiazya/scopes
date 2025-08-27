package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for retrieving a scope by its alias name.
 * This supports both canonical and custom aliases.
 */
class GetScopeByAliasHandler(
    private val aliasRepository: ScopeAliasRepository,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<GetScopeByAliasQuery, ScopesError, ScopeDto> {

    override suspend operator fun invoke(query: GetScopeByAliasQuery): Either<ScopesError, ScopeDto> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Getting scope by alias",
                mapOf("aliasName" to query.aliasName),
            )

            // Validate alias name
            val aliasName = AliasName.create(query.aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid alias name",
                        mapOf(
                            "aliasName" to query.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(query.aliasName)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(query.aliasName, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(query.aliasName, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(query.aliasName, error.expectedPattern)
                    }
                }
                .bind()

            // Find alias
            val alias = aliasRepository.findByAliasName(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find alias",
                        mapOf(
                            "aliasName" to query.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (alias == null) {
                logger.info(
                    "Alias not found",
                    mapOf("aliasName" to query.aliasName),
                )
                raise(ScopeAliasError.AliasNotFound(query.aliasName))
            }

            // Get scope by ID
            val scope = scopeRepository.findById(alias.scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find scope",
                        mapOf(
                            "scopeId" to alias.scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (scope == null) {
                logger.warn(
                    "Scope not found for alias",
                    mapOf(
                        "aliasName" to query.aliasName,
                        "scopeId" to alias.scopeId.value,
                    ),
                )
                // This is an inconsistency - alias exists but scope doesn't
                raise(ScopeAliasError.AliasNotFound(query.aliasName))
            }

            // Get all aliases for the scope to include in response
            val aliases = aliasRepository.findByScopeId(scope.id)
                .mapLeft { error ->
                    logger.warn(
                        "Failed to get aliases for scope",
                        mapOf(
                            "scopeId" to scope.id.value,
                            "error" to error.toString(),
                        ),
                    )
                    // Continue with empty list on failure
                    error.toApplicationError()
                }
                .getOrElse { emptyList() }

            logger.info(
                "Successfully retrieved scope by alias",
                mapOf(
                    "aliasName" to query.aliasName,
                    "scopeId" to scope.id.value,
                ),
            )

            val canonicalAlias = aliases.find { it.isCanonical() }?.aliasName?.value
            val customAliases = aliases.filter { !it.isCanonical() }.map { it.aliasName.value }

            ScopeMapper.toDto(scope, canonicalAlias, customAliases)
        }
    }
}
