package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.scopemanagement.application.dto.alias.AliasInfoDto
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.scope.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias

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
}
