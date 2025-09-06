package io.github.kamiazya.scopes.scopemanagement.application.mapper

import io.github.kamiazya.scopes.scopemanagement.application.dto.context.ContextViewDto
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView

/**
 * Mapper for converting between ContextView domain entities and DTOs.
 */
class ContextViewMapper {

    /**
     * Convert a domain entity to a DTO.
     */
    fun toDto(contextView: ContextView): ContextViewDto = ContextViewDto(
        id = contextView.id.value,
        key = contextView.key.value,
        name = contextView.name.value,
        filter = contextView.filter.expression,
        description = contextView.description?.value,
        createdAt = contextView.createdAt,
        updatedAt = contextView.updatedAt,
    )

    /**
     * Convert a list of domain entities to DTOs.
     */
    fun toDtos(contextViews: List<ContextView>): List<ContextViewDto> = contextViews.map { toDto(it) }
}
