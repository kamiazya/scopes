package io.github.kamiazya.scopes.application.mapper

import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.dto.ContextViewListResult
import io.github.kamiazya.scopes.application.dto.ScopeResult
import io.github.kamiazya.scopes.domain.entity.ContextView
import io.github.kamiazya.scopes.domain.entity.Scope

/**
 * Mapper between domain entities and application DTOs for context views.
 * Prevents domain entities from leaking to presentation layer.
 */
object ContextViewMapper {
    
    /**
     * Map ContextView entity to ContextViewResult DTO.
     */
    fun toResult(
        contextView: ContextView,
        isActive: Boolean = false
    ): ContextViewResult = ContextViewResult(
        id = contextView.id.toString(),
        name = contextView.name.value,
        filterExpression = contextView.filter.expression,
        description = contextView.description?.value,
        isActive = isActive,
        createdAt = contextView.createdAt,
        updatedAt = contextView.updatedAt
    )
    
    /**
     * Map list of ContextView entities to ContextViewListResult DTO.
     */
    fun toListResult(
        contextViews: List<ContextView>,
        activeContext: ContextView? = null
    ): ContextViewListResult {
        val results = contextViews.map { context ->
            toResult(context, isActive = context.id == activeContext?.id)
        }
        
        val activeResult = activeContext?.let { toResult(it, isActive = true) }
        
        return ContextViewListResult(
            contexts = results,
            activeContext = activeResult
        )
    }
    
    /**
     * Map Scope entity to ScopeResult DTO for filtered views.
     */
    fun toScopeResult(scope: Scope): ScopeResult = ScopeResult(
        id = scope.id.toString(),
        title = scope.title.value,
        description = scope.description?.value,
        parentId = scope.parentId?.toString(),
        aspects = scope.getAspects().mapKeys { it.key.value }
            .mapValues { it.value.toList().map { aspectValue -> aspectValue.value } },
        createdAt = scope.createdAt,
        updatedAt = scope.updatedAt
    )
}