package io.github.kamiazya.scopes.scopemanagement.application.query.handler.scope

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
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
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : QueryHandler<GetScopeByAlias, ScopeContractError, ScopeResult?> {

    override suspend operator fun invoke(input: GetScopeByAlias): Either<ScopeContractError, ScopeResult?> = transactionManager.inReadOnlyTransaction {
        logger.debug(
            "Getting scope by alias",
            mapOf(
                "aliasName" to input.aliasName,
            ),
        )
        either {
            // Validate and create alias value object
            val aliasName = AliasName.create(input.aliasName)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = input.aliasName),
                    )
                }
                .bind()

            // Find alias entity
            val scopeAlias = scopeAliasRepository.findByAliasName(aliasName)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }
                .bind()

            // If alias found, get the scope
            scopeAlias?.let { alias ->
                val scope = scopeRepository.findById(alias.scopeId)
                    .mapLeft { error ->
                        applicationErrorMapper.mapDomainError(error)
                    }
                    .bind()

                val result = scope?.let { s ->
                    // We already have the alias, but need to check if it's canonical
                    val canonicalAlias = if (alias.isCanonical()) {
                        alias.aliasName.value
                    } else {
                        // Need to fetch the canonical alias
                        val canonical = scopeAliasRepository.findCanonicalByScopeId(s.id)
                            .mapLeft { error ->
                                applicationErrorMapper.mapDomainError(error)
                            }
                            .bind()

                        // Missing canonical alias is a data consistency error
                        if (canonical == null) {
                            raise(
                                ScopeContractError.DataInconsistency.MissingCanonicalAlias(
                                    scopeId = s.id.toString(),
                                ),
                            )
                        }
                        canonical.aliasName.value
                    }

                    ScopeResult(
                        id = s.id.toString(),
                        title = s.title.value,
                        description = s.description?.value,
                        parentId = s.parentId?.toString(),
                        canonicalAlias = canonicalAlias,
                        createdAt = s.createdAt,
                        updatedAt = s.updatedAt,
                        isArchived = false,
                        aspects = s.aspects.toMap().mapKeys { (key, _) ->
                            key.value
                        }.mapValues { (_, values) ->
                            values.map { it.value }
                        },
                    )
                }

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
                "aliasName" to input.aliasName,
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }
}
