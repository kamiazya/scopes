package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ScopeMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeByAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for retrieving a scope by its alias name.
 *
 * This handler validates the alias, retrieves the scope from the repository,
 * and maps it to a DTO for external consumption.
 */
class GetScopeByAliasHandler(
    private val scopeAliasRepository: ScopeAliasRepository,
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : QueryHandler<GetScopeByAlias, ScopeManagementApplicationError, ScopeDto?> {

    override suspend operator fun invoke(query: GetScopeByAlias): Either<ScopeManagementApplicationError, ScopeDto?> =
        transactionManager.inReadOnlyTransaction {
            logger.debug(
                "Getting scope by alias",
                mapOf(
                    "aliasName" to query.aliasName,
                ),
            )
            either {
                // Validate and create alias value object
                val aliasName = AliasName.create(query.aliasName)
                    .mapLeft { it.toGenericApplicationError() }
                    .bind()

                // Find alias entity
                val scopeAlias = scopeAliasRepository.findByAliasName(aliasName)
                    .mapLeft { _ ->
                        ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                            operation = "findByAliasName",
                        )
                    }
                    .bind()

                // If alias found, get the scope
                scopeAlias?.let { alias ->
                    val scope = scopeRepository.findById(alias.scopeId)
                        .mapLeft { _ ->
                            ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                                operation = "findById",
                            )
                        }
                        .bind()

                    val result = scope?.let { ScopeMapper.toDto(it) }

                    logger.info(
                        "Scope by alias lookup completed",
                        mapOf(
                            "aliasName" to aliasName.value,
                            "scopeId" to (alias.scopeId.value.toString()),
                            "found" to (result != null).toString(),
                        ),
                    )

                    result
                } ?: run {
                    logger.info(
                        "Alias not found",
                        mapOf(
                            "aliasName" to aliasName.value,
                        ),
                    )
                    null
                }
            }
        }.onLeft { error ->
            logger.error(
                "Failed to get scope by alias",
                mapOf(
                    "aliasName" to query.aliasName,
                    "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                    "message" to error.toString(),
                ),
            )
        }
}
