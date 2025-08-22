package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope

/**
 * Mapper between domain entities and application DTOs.
 * Prevents domain entities from leaking to presentation layer.
 */
object ScopeMapper {

    /**
     * Map Scope entity to CreateScopeResult DTO.
     */
    fun toCreateScopeResult(scope: Scope, canonicalAlias: String? = null): CreateScopeResult = CreateScopeResult(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        createdAt = scope.createdAt,
        canonicalAlias = canonicalAlias,
        aspects = scope.aspects.toMap().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } },
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
}
