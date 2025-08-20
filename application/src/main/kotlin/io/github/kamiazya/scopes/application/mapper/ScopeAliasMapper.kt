package io.github.kamiazya.scopes.application.mapper

import io.github.kamiazya.scopes.application.dto.ScopeAliasResult
import io.github.kamiazya.scopes.domain.entity.ScopeAlias

/**
 * Mapper for converting ScopeAlias domain entities to DTOs.
 *
 * This mapper handles the transformation between domain and application layers,
 * abstracting internal implementation details from presentation concerns.
 */
object ScopeAliasMapper {

    /**
     * Converts a ScopeAlias domain entity to a DTO.
     */
    fun toDto(alias: ScopeAlias): ScopeAliasResult = ScopeAliasResult(
        scopeId = alias.scopeId.value,
        aliasName = alias.aliasName.value,
        aliasType = alias.aliasType.name,
        createdAt = alias.createdAt,
        updatedAt = alias.updatedAt,
    )

    /**
     * Converts a list of ScopeAlias domain entities to DTOs.
     */
    fun toDtoList(aliases: List<ScopeAlias>): List<ScopeAliasResult> = aliases.map { toDto(it) }
}
