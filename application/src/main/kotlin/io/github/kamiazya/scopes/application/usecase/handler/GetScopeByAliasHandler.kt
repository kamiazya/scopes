package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.ScopeDTO
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.ScopesError
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName

/**
 * Handler for GetScopeByAlias query.
 * Retrieves a scope using its alias name (canonical or custom).
 *
 * This handler abstracts away the internal ULID implementation,
 * allowing users to work with human-readable aliases.
 */
class GetScopeByAliasHandler(
    private val scopeRepository: ScopeRepository,
    private val aliasManagementService: ScopeAliasManagementService,
) : UseCase<GetScopeByAliasQuery, ScopesError, ScopeDTO> {

    override suspend operator fun invoke(input: GetScopeByAliasQuery): Either<ScopesError, ScopeDTO> = either {
        // Parse the alias name
        val aliasName = AliasName.create(input.aliasName).bind()

        // Resolve the alias to a scope ID
        val scopeId = aliasManagementService.resolveAlias(aliasName).bind()

        // Fetch the scope
        val scopeResult = scopeRepository.findById(scopeId).bind()

        // Handle nullable scope
        if (scopeResult == null) {
            raise(
                PersistenceError.DataCorruption(
                    occurredAt = kotlinx.datetime.Clock.System.now(),
                    entityType = "Scope",
                    entityId = scopeId.toString(),
                    reason = "Scope not found for resolved alias",
                ),
            )
        }

        val scope = scopeResult

        // Get all aliases for the scope
        val aliases = aliasManagementService.getAliasesForScope(scopeId)
            .getOrNull() ?: emptyList()

        // Find the canonical alias
        val canonicalAlias = aliases.find { it.isCanonical() }

        // Map to DTO
        ScopeDTO(
            id = scope.id.toString(),
            title = scope.title.value,
            description = scope.description?.value,
            parentId = scope.parentId?.toString(),
            canonicalAlias = canonicalAlias?.aliasName?.value,
            customAliases = aliases.filter { it.isCustom() }.map { it.aliasName.value },
            createdAt = scope.createdAt,
            updatedAt = scope.updatedAt,
            aspects = scope.getAspects().mapKeys {
                it.key.value
            }.mapValues { it.value.toList().map { v -> v.value } },
        )
    }
}
