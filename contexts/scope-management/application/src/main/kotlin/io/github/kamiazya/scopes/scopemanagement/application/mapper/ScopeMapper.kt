package io.github.kamiazya.scopes.scopemanagement.application.mapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.alias.AliasInfoDto
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope

/**
 * Mapper between domain entities and application DTOs.
 * Prevents domain entities from leaking to presentation layer.
 */
object ScopeMapper {

    /**
     * Map Scope entity to CreateScopeResult DTO (contract layer).
     */
    fun toCreateScopeResult(scope: Scope, canonicalAlias: String): CreateScopeResult = CreateScopeResult(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        canonicalAlias = canonicalAlias,
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
    )

    /**
     * Map Scope entity to ScopeDto.
     */
    fun toDto(scope: Scope): ScopeDto = ScopeDto(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
        aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
    )

    /**
     * Map Scope entity to ScopeDto with alias information.
     */
    fun toDto(scope: Scope, canonicalAlias: String? = null, customAliases: List<String> = emptyList()): ScopeDto = ScopeDto(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        canonicalAlias = canonicalAlias,
        customAliases = customAliases,
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
        aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
    )

    /**
     * Map Scope entity to ScopeDto with alias entities.
     */
    fun toDto(scope: Scope, aliases: List<ScopeAlias>): ScopeDto {
        val aliasDtos = aliases.map { alias ->
            AliasInfoDto(
                aliasName = alias.aliasName.value,
                aliasType = alias.aliasType.name,
                isCanonical = alias.isCanonical(),
                createdAt = alias.createdAt,
            )
        }

        // Sort aliases - canonical first, then by name
        val sortedAliases = aliasDtos.sortedWith(
            compareByDescending<AliasInfoDto> { it.isCanonical }
                .thenBy { it.aliasName },
        )

        return ScopeDto(
            id = scope.id.toString(),
            title = scope.title.value,
            description = scope.description?.value,
            parentId = scope.parentId?.toString(),
            canonicalAlias = sortedAliases.find { it.isCanonical }?.aliasName,
            customAliases = sortedAliases.filterNot { it.isCanonical }.map { it.aliasName },
            createdAt = scope.createdAt,
            updatedAt = scope.updatedAt,
            aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
        )
    }

    /**
     * Map Scope entity to ScopeResult.
     * This method maps to the contract layer DTO for external clients.
     * Returns Either to handle missing canonical alias consistently with the error handling pattern.
     */
    fun toScopeResult(scope: Scope, aliases: List<ScopeAlias>): Either<ScopeContractError, ScopeResult> {
        val canonicalAlias = aliases.find { it.isCanonical() }?.aliasName?.value
            ?: return ScopeContractError.DataInconsistency.MissingCanonicalAlias(scopeId = scope.id.toString()).left()

        return ScopeResult(
            id = scope.id.toString(),
            title = scope.title.value,
            description = scope.description?.value,
            parentId = scope.parentId?.toString(),
            canonicalAlias = canonicalAlias,
            createdAt = scope.createdAt,
            updatedAt = scope.updatedAt,
            isArchived = false, // Default value, can be updated based on business logic
            aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
        ).right()
    }

    /**
     * Map Scope entity to ScopeResult with explicit canonical alias.
     * This method is for cases where the canonical alias is already known.
     */
    fun toScopeResult(scope: Scope, canonicalAlias: String): ScopeResult = ScopeResult(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        canonicalAlias = canonicalAlias,
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt,
        isArchived = false, // Default value, can be updated based on business logic
        aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
    )
}
