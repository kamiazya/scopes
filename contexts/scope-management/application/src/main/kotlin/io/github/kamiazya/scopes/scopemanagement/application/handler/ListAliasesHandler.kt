package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.AliasInfoDto
import io.github.kamiazya.scopes.scopemanagement.application.dto.AliasListDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for listing all aliases associated with a scope.
 * Returns aliases sorted with canonical first, then by creation date.
 */
class ListAliasesHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<ListAliases, ScopesError, AliasListDto> {

    override suspend operator fun invoke(input: ListAliases): Either<ScopesError, AliasListDto> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Listing aliases for scope",
                mapOf("scopeId" to input.scopeId),
            )

            // Validate scopeId - fast-path check for blank/empty
            if (input.scopeId.isBlank()) {
                logger.error(
                    "Scope ID is blank",
                    mapOf("scopeId" to input.scopeId),
                )
                raise(ScopeInputError.IdBlank(input.scopeId))
            }

            val scopeId = ScopeId.create(input.scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Invalid scope ID format",
                        mapOf(
                            "scopeId" to input.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    // After blank check, remaining errors should be format-related
                    ScopeInputError.IdInvalidFormat(input.scopeId, "ULID")
                }
                .bind()

            // Retrieve all aliases for the scope through domain service
            val aliases = scopeAliasService.getAliasesForScope(scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Failed to retrieve aliases",
                        mapOf(
                            "scopeId" to input.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
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
                    "scopeId" to input.scopeId,
                    "count" to aliases.size,
                    "hasCanonical" to aliases.any { it.isCanonical() },
                ),
            )

            AliasListDto(
                scopeId = input.scopeId,
                aliases = aliasDtos,
                totalCount = aliasDtos.size,
            )
        }
    }
}
