package io.github.kamiazya.scopes.application.mapper

import io.github.kamiazya.scopes.application.dto.ScopeAliasDTO
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
    fun toDto(alias: ScopeAlias): ScopeAliasDTO = ScopeAliasDTO(
        scopeId = alias.scopeId.value,
        aliasName = alias.aliasName.value,
        aliasType = alias.aliasType.name,
        createdAt = alias.createdAt,
        updatedAt = alias.updatedAt
    )
    
    /**
     * Converts a list of ScopeAlias domain entities to DTOs.
     */
    fun toDtoList(aliases: List<ScopeAlias>): List<ScopeAliasDTO> = 
        aliases.map { toDto(it) }
}