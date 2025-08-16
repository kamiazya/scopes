package io.github.kamiazya.scopes.application.mapper

import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.domain.entity.Scope

/**
 * Mapper between domain entities and application DTOs.
 * Prevents domain entities from leaking to presentation layer.
 */
object ScopeMapper {

    /**
     * Map Scope entity to CreateScopeResult DTO.
     */
    fun toCreateScopeResult(scope: Scope): CreateScopeResult =
        CreateScopeResult(
            id = scope.id.toString(),
            title = scope.title.value,
            description = scope.description?.value,
            parentId = scope.parentId?.toString(),
            createdAt = scope.createdAt,
            aspects = scope.getAspects().mapKeys { it.key.value }.mapValues { it.value.toList().map { v -> v.value } }
        )

}
