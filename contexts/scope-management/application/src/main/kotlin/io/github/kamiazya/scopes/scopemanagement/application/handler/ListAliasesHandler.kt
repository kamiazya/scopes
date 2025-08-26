package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.AliasInfoDto
import io.github.kamiazya.scopes.scopemanagement.application.dto.AliasListDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for listing all aliases associated with a scope.
 * Returns aliases sorted with canonical first, then by creation date.
 */
class ListAliasesHandler(private val aliasRepository: ScopeAliasRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    UseCase<ListAliases, ScopesError, AliasListDto> {

    override suspend operator fun invoke(query: ListAliases): Either<ScopesError, AliasListDto> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Listing aliases for scope",
                mapOf("scopeId" to query.scopeId),
            )

            // Validate scopeId - fast-path check for blank/empty
            if (query.scopeId.isBlank()) {
                logger.error(
                    "Scope ID is blank",
                    mapOf("scopeId" to query.scopeId),
                )
                raise(ScopeInputError.IdBlank(query.scopeId))
            }

            val scopeId = ScopeId.create(query.scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Invalid scope ID format",
                        mapOf(
                            "scopeId" to query.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    // After blank check, remaining errors should be format-related
                    ScopeInputError.IdInvalidFormat(query.scopeId, "ULID")
                }
                .bind()

            // Retrieve all aliases for the scope
            val aliases = aliasRepository.findByScopeId(scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Failed to retrieve aliases",
                        mapOf(
                            "scopeId" to query.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            // Map to DTOs and sort
            val aliasDtos = aliases
                .map { alias ->
                    AliasInfoDto(
                        aliasName = alias.aliasName.value,
                        aliasType = alias.aliasType.name,
                        isCanonical = alias.isCanonical(),
                        createdAt = alias.createdAt,
                    )
                }
                .sortedWith(
                    compareBy(
                        { !it.isCanonical }, // Canonical first (false < true)
                        { it.createdAt }, // Then by creation date
                    ),
                )

            logger.info(
                "Successfully retrieved aliases",
                mapOf(
                    "scopeId" to query.scopeId,
                    "count" to aliases.size,
                    "hasCanonical" to aliases.any { it.isCanonical() },
                ),
            )

            AliasListDto(
                scopeId = query.scopeId,
                aliases = aliasDtos,
                totalCount = aliasDtos.size,
            )
        }
    }
}
