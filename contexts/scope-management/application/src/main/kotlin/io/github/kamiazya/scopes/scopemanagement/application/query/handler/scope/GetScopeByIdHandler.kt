package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.BaseQueryHandler
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for GetScopeById query.
 * Uses BaseQueryHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 * Retrieves a scope by its ID and returns it as a DTO.
 */
class GetScopeByIdHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasRepository: ScopeAliasRepository,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseQueryHandler<GetScopeById, ScopeResult?>(transactionManager, logger) {

    override suspend fun executeQuery(query: GetScopeById): Either<ScopeContractError, ScopeResult?> = either {
        logger.debug(
            "Getting scope by ID",
            mapOf("scopeId" to query.id),
        )

        // Parse and validate the scope ID
        val scopeId = ScopeId.create(query.id)
            .mapLeft { error ->
                logger.error(
                    "Invalid scope ID format",
                    mapOf(
                        "scopeId" to query.id,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(
                    error,
                    ErrorMappingContext(attemptedValue = query.id),
                )
            }
            .bind()

        // Retrieve the scope from repository
        val scope = scopeRepository.findById(scopeId)
            .mapLeft { error ->
                logger.error(
                    "Failed to find scope by ID",
                    mapOf(
                        "scopeId" to query.id,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(error)
            }
            .bind()

        // Map to Contract DTO if found
        val result = if (scope != null) {
            // Get canonical alias for this scope
            val canonicalAlias = aliasRepository.findCanonicalByScopeId(scope.id)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find canonical alias",
                        mapOf(
                            "scopeId" to scope.id.toString(),
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()

            // Missing canonical alias is a data consistency error
            if (canonicalAlias == null) {
                logger.error(
                    "Missing canonical alias for scope",
                    mapOf("scopeId" to scope.id.toString()),
                )
                raise(
                    ScopeContractError.DataInconsistency.MissingCanonicalAlias(
                        scopeId = scope.id.toString(),
                    ),
                )
            }

            ScopeResult(
                id = scope.id.toString(),
                title = scope.title.value,
                description = scope.description?.value,
                parentId = scope.parentId?.toString(),
                canonicalAlias = canonicalAlias.aliasName.value,
                createdAt = scope.createdAt,
                updatedAt = scope.updatedAt,
                isArchived = false,
                aspects = scope.aspects.toMap().mapKeys { (key, _) ->
                    key.value
                }.mapValues { (_, values) ->
                    values.map { it.value }
                },
            )
        } else {
            null
        }

        logger.info(
            "Scope lookup completed",
            mapOf(
                "scopeId" to scopeId.value.toString(),
                "found" to (result != null).toString(),
            ),
        )

        result
    }
}
